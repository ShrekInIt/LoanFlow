package com.example.applicationservice.service;

import com.example.applicationservice.application.ApplicationEntity;
import com.example.applicationservice.application.ApplicationMapper;
import com.example.applicationservice.application.ApplicationRepository;
import com.example.applicationservice.application.service.ApplicationServiceImpl;
import com.example.applicationservice.application.web.ApplicationResponse;
import com.example.applicationservice.application.web.CreateApplicationRequest;
import com.example.applicationservice.metrics.ApplicationMetrics;
import com.example.applicationservice.outbox.OutboxService;
import com.example.enums.ApplicationStatus;
import com.example.event.ScoringCompletedEvent;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceImplTest {
    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ApplicationMetrics applicationMetrics;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void getApplicationById_notFound_throwException(){
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updateStatus_whenTransitionAllowed_shouldChangeStatus(){
        ApplicationEntity entity = getEntity(ApplicationStatus.NEW);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        ApplicationResponse response = getResponse(entity, ApplicationStatus.SCORING_IN_PROGRESS);

        when(applicationMapper.toResponse(entity))
                .thenReturn(response);

        ApplicationResponse actual = applicationService
                .updateApplicationStatus(1L, ApplicationStatus.SCORING_IN_PROGRESS);

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.SCORING_IN_PROGRESS);
        assertThat(actual.status()).isEqualTo(ApplicationStatus.SCORING_IN_PROGRESS);

        verify(applicationRepository).findById(1L);
        verify(applicationMapper).toResponse(entity);
        verify(applicationRepository).save(entity);
    }

    @Test
    void updateStatus_whenTransitionFailed_shouldNotChangeStatus(){
        ApplicationEntity entity = getEntity(ApplicationStatus.NEW);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(1L, ApplicationStatus.ISSUED)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Невозможно перейти");

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.NEW);
        verify(applicationRepository).findById(1L);
        verify(applicationRepository, never()).save(any());
        verify(applicationMapper, never()).toResponse(any());

    }

    @Test
    void updateStatus_whenCurrentStatusIsFinal_shouldThrowExceptionAndNotChangeStatus() {
        ApplicationEntity entity = getEntity(ApplicationStatus.FAILED);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(1L, ApplicationStatus.ISSUED)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Невозможно перейти");

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.FAILED);
        verify(applicationRepository).findById(1L);
        verify(applicationRepository, never()).save(any());
        verify(applicationMapper, never()).toResponse(any());
    }

    @Test
    void updateStatus_whenApplicationNotFound_shouldThrowException(){
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(1L, ApplicationStatus.ISSUED)
        )
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");

        verify(applicationRepository).findById(1L);
        verify(applicationRepository, never()).save(any());
        verify(applicationMapper, never()).toResponse(any());
    }

    @Test
    void createApplication_success(){
        CreateApplicationRequest request = getCreateRequest();
        ApplicationEntity entity = getEntity(ApplicationStatus.NEW);

        ApplicationResponse response = getResponse(entity, ApplicationStatus.SCORING_IN_PROGRESS);

        when(applicationRepository.existsByFullNameAndStatus(
                request.fullName(),
                ApplicationStatus.NEW
        )).thenReturn(false);

        when(applicationRepository.existsByFullNameAndCreatedAtAfter(
                eq(request.fullName()),
                any(LocalDateTime.class)
        )).thenReturn(false);

        when(applicationMapper.toEntity(request))
                .thenReturn(entity);

        when(applicationRepository.save(entity))
                .thenReturn(entity);

        when(applicationMapper.toResponse(entity))
                .thenReturn(response);

        ApplicationResponse actual = applicationService.createApplication(request);

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.SCORING_IN_PROGRESS);
        assertThat(actual).isEqualTo(response);
        assertThat(actual.status()).isEqualTo(ApplicationStatus.SCORING_IN_PROGRESS);

        verify(applicationRepository).existsByFullNameAndStatus(
                request.fullName(),
                ApplicationStatus.NEW
        );

        verify(applicationRepository).existsByFullNameAndCreatedAtAfter(
                eq(request.fullName()),
                any(LocalDateTime.class)
        );

        verify(applicationMapper).toEntity(request);

        verify(applicationRepository, times(2)).save(entity);

        verify(outboxService).saveApplicationCreatedEvent(entity);
        verify(applicationMetrics).incrementApplicationCreated();
        verify(applicationMapper).toResponse(entity);
    }

    @Test
    void createApplication_whenNewApplicationExists_shouldThrowException(){
        CreateApplicationRequest request = getCreateRequest();

        when(applicationRepository.existsByFullNameAndStatus(
                request.fullName(),
                ApplicationStatus.NEW
        )).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже существует");

        verify(applicationRepository).existsByFullNameAndStatus(
                request.fullName(),
                ApplicationStatus.NEW
        );

        verify(applicationRepository, never())
                .existsByFullNameAndCreatedAtAfter(any(), any());

        verify(applicationMapper, never()).toEntity(any());
        verify(applicationRepository, never()).save(any());
        verify(applicationMapper, never()).toResponse(any());
        verify(outboxService, never()).saveApplicationCreatedEvent(any());
    }

    @Test
    void createApplication_whenApplicationCreatedLessThanDayAgo_shouldThrowException(){
        CreateApplicationRequest request = getCreateRequest();

        when(applicationRepository.existsByFullNameAndStatus(
                request.fullName(),
                ApplicationStatus.NEW
        )).thenReturn(false);

        when(applicationRepository.existsByFullNameAndCreatedAtAfter(
                eq(request.fullName()),
                any(LocalDateTime.class)
        )).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("менее 1 дня назад");

        verify(applicationRepository).existsByFullNameAndStatus(
                request.fullName(),
                ApplicationStatus.NEW
        );

        verify(applicationRepository).existsByFullNameAndCreatedAtAfter(
                eq(request.fullName()),
                any(LocalDateTime.class)
        );

        verify(applicationMapper, never()).toEntity(any());
        verify(applicationRepository, never()).save(any());
        verify(applicationMapper, never()).toResponse(any());
        verify(outboxService, never()).saveApplicationCreatedEvent(any());

    }

    @Test
    void getApplicationById_success(){
        ApplicationEntity entity = getEntity(ApplicationStatus.NEW);
        ApplicationResponse response = getResponse(entity, ApplicationStatus.NEW);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        when(applicationMapper.toResponse(entity))
                .thenReturn(response);

        ApplicationResponse actual = applicationService
                .getApplicationById(1L);

        assertThat(actual).isEqualTo(response);
        assertThat(actual.status()).isEqualTo(ApplicationStatus.NEW);

        verify(applicationRepository).findById(1L);
        verify(applicationMapper).toResponse(entity);
    }

    @Test
    void getAllApplications_shouldMapEveryEntity() {
        ApplicationEntity first = getEntity(ApplicationStatus.NEW);
        ApplicationEntity second = getEntity(ApplicationStatus.APPROVED);
        second.setId(2L);
        ApplicationResponse firstResponse = getResponse(first, ApplicationStatus.NEW);
        ApplicationResponse secondResponse = getResponse(second, ApplicationStatus.APPROVED);

        when(applicationRepository.findAll()).thenReturn(List.of(first, second));
        when(applicationMapper.toResponse(first)).thenReturn(firstResponse);
        when(applicationMapper.toResponse(second)).thenReturn(secondResponse);

        assertThat(applicationService.getAllApplications())
                .isEqualTo(List.of(firstResponse, secondResponse));
    }

    @Test
    void  processScoringResult_whenApproved_shouldMoveToApproved() {
        ApplicationEntity entity = getEntity(ApplicationStatus.SCORING_IN_PROGRESS);
        ScoringCompletedEvent event = getScoringCompletedEvent(true);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        when(applicationRepository.save(entity))
                .thenReturn(entity);

        applicationService.processScoringResult(event);

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(entity.getFailureReason()).isNull();

        verify(applicationRepository).findById(1L);
        verify(applicationRepository).save(entity);
        verify(applicationMetrics).incrementScoringApproved();
    }

    @Test
    void processScoringResult_whenRejected_shouldMoveToRejected() {
        ApplicationEntity entity = getEntity(ApplicationStatus.SCORING_IN_PROGRESS);
        ScoringCompletedEvent event = getScoringCompletedEvent(false);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        when(applicationRepository.save(entity))
                .thenReturn(entity);

        applicationService.processScoringResult(event);

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(entity.getFailureReason()).isEqualTo("Rejected");

        verify(applicationRepository).findById(1L);
        verify(applicationRepository).save(entity);
        verify(applicationMetrics).incrementScoringRejected();
    }

    @Test
    void processScoringResult_whenApplicationInWrongStatus_shouldThrowException() {
        ApplicationEntity entity = getEntity(ApplicationStatus.NEW);
        ScoringCompletedEvent event = getScoringCompletedEvent(true);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> applicationService.processScoringResult(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Невозможно перейти");

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.NEW);

        verify(applicationRepository).findById(1L);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void issueApplication_whenApproved_shouldMoveToIssued() {
        ApplicationEntity entity = getEntity(ApplicationStatus.APPROVED);
        ApplicationResponse response = getResponse(entity, ApplicationStatus.ISSUED);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        when(applicationRepository.save(entity))
                .thenReturn(entity);

        when(applicationMapper.toResponse(entity))
                .thenReturn(response);

        ApplicationResponse actual = applicationService.issueApplication(1L);

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.ISSUED);
        assertThat(actual.status()).isEqualTo(ApplicationStatus.ISSUED);

        verify(applicationRepository).findById(1L);
        verify(applicationRepository).save(entity);
        verify(applicationMapper).toResponse(entity);
    }

    @Test
    void issueApplication_whenRejected_shouldThrowException() {
        ApplicationEntity entity = getEntity(ApplicationStatus.REJECTED);

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> applicationService.issueApplication(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Невозможно перейти");

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.REJECTED);

        verify(applicationRepository).findById(1L);
        verify(applicationRepository, never()).save(any());
        verify(applicationMapper, never()).toResponse(any());
    }

    @Test
    void failApplication_whenTransitionAllowed_shouldStoreReason() {
        ApplicationEntity entity = getEntity(ApplicationStatus.APPROVED);
        ApplicationResponse response = getResponse(entity, ApplicationStatus.FAILED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(applicationRepository.save(entity)).thenReturn(entity);
        when(applicationMapper.toResponse(entity)).thenReturn(response);

        ApplicationResponse actual = applicationService.failApplication(1L, "Manual failure");

        assertThat(actual).isEqualTo(response);
        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.FAILED);
        assertThat(entity.getFailureReason()).isEqualTo("Manual failure");
    }

    @Test
    void cancelIssuedApplication_whenIssued_shouldStoreReasonAndPublishEvent() {
        ApplicationEntity entity = getEntity(ApplicationStatus.ISSUED);
        ApplicationResponse response = getResponse(entity, ApplicationStatus.ISSUE_CANCELLED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(applicationRepository.save(entity)).thenReturn(entity);
        when(applicationMapper.toResponse(entity)).thenReturn(response);

        ApplicationResponse actual = applicationService.cancelIssuedApplication(1L, "Notification failed");

        assertThat(actual).isEqualTo(response);
        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.ISSUE_CANCELLED);
        assertThat(entity.getFailureReason()).isEqualTo("Notification failed");
        verify(outboxService).saveIssueCancelledEvent(entity);
    }

    private ApplicationEntity getEntity(
            ApplicationStatus status
    ){
        return ApplicationEntity.builder()
                .id(1L)
                .fullName("John Boy Boy")
                .salary(BigDecimal.valueOf(100000))
                .creditAmount(BigDecimal.valueOf(50000))
                .status(status)
                .creditPurpose("Buying family car")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0)
                .build();
    }

    private ScoringCompletedEvent getScoringCompletedEvent(boolean approved) {
        return new ScoringCompletedEvent(
                "scoring-event-id",
                "source-event-id",
                1L,
                approved,
                approved ? "Approved" : "Rejected",
                LocalDateTime.now()
        );
    }

    private ApplicationResponse getResponse(
            ApplicationEntity entity,
            ApplicationStatus status
    ){
        return ApplicationResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .salary(entity.getSalary())
                .creditAmount(entity.getCreditAmount())
                .creditPurpose(entity.getCreditPurpose())
                .status(status)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CreateApplicationRequest getCreateRequest(){
        return CreateApplicationRequest.builder()
                .fullName("John")
                .salary(BigDecimal.valueOf(100000))
                .creditAmount(BigDecimal.valueOf(50000))
                .creditPurpose("Buying family car")
                .build();
    }

}

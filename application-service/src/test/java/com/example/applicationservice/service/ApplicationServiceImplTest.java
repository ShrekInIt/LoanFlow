package com.example.applicationservice.service;

import com.example.applicationservice.application.ApplicationEntity;
import com.example.applicationservice.application.ApplicationMapper;
import com.example.applicationservice.application.ApplicationRepository;
import com.example.applicationservice.application.enums.ApplicationStatus;
import com.example.applicationservice.application.service.ApplicationServiceImpl;
import com.example.applicationservice.application.web.ApplicationResponse;
import com.example.applicationservice.application.web.CreateApplicationRequest;
import com.example.applicationservice.kafka.ApplicationEventPublisher;
import com.example.applicationservice.kafka.event.ApplicationCreatedEvent;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private ApplicationEventPublisher eventPublisher;

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
        ApplicationResponse response = getResponse(entity, ApplicationStatus.NEW);
        ApplicationCreatedEvent event = getEvent(entity);

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
        when(applicationMapper.toResponse(entity))
                .thenReturn(response);
        when(applicationMapper.toApplicationCreatedEvent(entity))
                .thenReturn(event);

        ApplicationResponse actual = applicationService.createApplication(request);

        assertThat(actual).isEqualTo(response);
        assertThat(actual.status()).isEqualTo(ApplicationStatus.NEW);

        verify(applicationRepository).existsByFullNameAndCreatedAtAfter(
                eq(request.fullName()),
                any(LocalDateTime.class)
        );
        verify(applicationRepository)
                .existsByFullNameAndStatus(
                        request.fullName(),
                        ApplicationStatus.NEW
        );
        verify(applicationMapper).toEntity(request);
        verify(applicationRepository).save(entity);
        verify(applicationMapper).toResponse(entity);
        verify(eventPublisher).publishApplicationCreated(any(ApplicationCreatedEvent.class));
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
        verify(eventPublisher, never()).publishApplicationCreated(any());
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
        verify(eventPublisher, never()).publishApplicationCreated(any());
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

    private ApplicationCreatedEvent getEvent(ApplicationEntity entity){
        return ApplicationCreatedEvent.builder()
                .eventId("Yippso-02992")
                .applicationId(entity.getId())
                .fullName(entity.getFullName())
                .salary(entity.getSalary())
                .creditPurpose(entity.getCreditPurpose())
                .status(entity.getStatus())
                .creditAmount(entity.getCreditAmount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

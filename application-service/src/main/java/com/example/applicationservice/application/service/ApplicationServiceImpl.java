package com.example.applicationservice.application.service;

import com.example.applicationservice.application.ApplicationEntity;
import com.example.applicationservice.application.ApplicationMapper;
import com.example.applicationservice.application.ApplicationRepository;
import com.example.applicationservice.application.web.ApplicationResponse;
import com.example.applicationservice.application.web.CreateApplicationRequest;
import com.example.applicationservice.outbox.OutboxService;
import com.example.enums.ApplicationStatus;
import com.example.event.ScoringCompletedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final Map<ApplicationStatus, List<ApplicationStatus>> statusTransition = Map.of(
            ApplicationStatus.NEW, List.of(ApplicationStatus.SCORING_IN_PROGRESS, ApplicationStatus.FAILED),
            ApplicationStatus.SCORING_IN_PROGRESS, List.of(ApplicationStatus.SCORING_APPROVED, ApplicationStatus.SCORING_REJECTED, ApplicationStatus.FAILED),
            ApplicationStatus.SCORING_APPROVED, List.of(ApplicationStatus.APPROVED, ApplicationStatus.FAILED),
            ApplicationStatus.SCORING_REJECTED, List.of(ApplicationStatus.REJECTED, ApplicationStatus.FAILED),
            ApplicationStatus.APPROVED, List.of(ApplicationStatus.ISSUED, ApplicationStatus.FAILED),
            ApplicationStatus.ISSUED, List.of(ApplicationStatus.ISSUE_CANCELLED)
    );
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final OutboxService outboxService;


    @Override
    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        if(applicationRepository.existsByFullNameAndStatus(request.fullName(), ApplicationStatus.NEW)){
            throw new IllegalStateException("Заявка с таким ФИО уже существует");
        }

        if (applicationRepository.existsByFullNameAndCreatedAtAfter(request.fullName(), LocalDateTime.now().minusDays(1))) {
            throw new IllegalStateException("Заявка с таким ФИО уже была создана менее 1 дня назад");
        }

        ApplicationEntity entity = applicationMapper.toEntity(request);
        ApplicationEntity savedEntity = applicationRepository.save(entity);

        savedEntity.setStatus(ApplicationStatus.SCORING_IN_PROGRESS);
        ApplicationEntity scoringEntity = applicationRepository.save(savedEntity);

        outboxService.saveApplicationCreatedEvent(scoringEntity);

        return applicationMapper.toResponse(scoringEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        return applicationMapper.toResponse(getApplicationEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplicationStatus(Long id, ApplicationStatus status) {
        ApplicationEntity entity = getApplicationEntity(id);

        moveStatus(entity, status);

        applicationRepository.save(entity);

        return applicationMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void processScoringResult(ScoringCompletedEvent event) {
        log.info("Получено событие о завершении скоринга для заявки с id: {}, результат скоринга: {}",
                event.applicationId(), event.approved());
        Long id = event.applicationId();
        ApplicationEntity entity = getApplicationEntity(id);
        if(event.approved()){
            moveStatus(entity, ApplicationStatus.SCORING_APPROVED);
            moveStatus(entity, ApplicationStatus.APPROVED);
            entity.setFailureReason(null);
        }else {
            moveStatus(entity, ApplicationStatus.SCORING_REJECTED);
            moveStatus(entity, ApplicationStatus.REJECTED);
            entity.setFailureReason(event.reason());
        }
        applicationRepository.save(entity);
        log.info("Заявка с id: {} обновлена после получения результата скоринга, новый статус: {}",
                id, entity.getStatus());
    }

    @Override
    @Transactional
    public ApplicationResponse issueApplication(Long id) {
        return updateApplicationStatus(id, ApplicationStatus.ISSUED);
    }

    @Override
    @Transactional
    public ApplicationResponse failApplication(Long id, String reason) {
        ApplicationEntity entity = getApplicationEntity(id);
        moveStatus(entity, ApplicationStatus.FAILED);
        entity.setFailureReason(reason);
        ApplicationEntity savedEntity = applicationRepository.save(entity);
        return applicationMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public ApplicationResponse cancelIssuedApplication(Long id, String reason) {
        ApplicationEntity entity = getApplicationEntity(id);
        moveStatus(entity, ApplicationStatus.ISSUE_CANCELLED);
        entity.setFailureReason(reason);
        ApplicationEntity savedEntity = applicationRepository.save(entity);
        outboxService.saveIssueCancelledEvent(savedEntity);
        return applicationMapper.toResponse(savedEntity);
    }

    private void moveStatus(ApplicationEntity entity, ApplicationStatus targetStatus) {
        ApplicationStatus currentStatus = entity.getStatus();

        List<ApplicationStatus> nextStatuses = statusTransition.getOrDefault(currentStatus, List.of());

        if (!nextStatuses.contains(targetStatus)) {
            throw new IllegalStateException(
                    "Невозможно перейти из статуса " + currentStatus + " в статус " + targetStatus
            );
        }

        entity.setStatus(targetStatus);
    }

    private ApplicationEntity getApplicationEntity(Long id){
        return applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка с ID " + id + " не найдена"));
    }
}

package com.example.applicationservice.application.service;

import com.example.applicationservice.application.ApplicationEntity;
import com.example.applicationservice.application.ApplicationMapper;
import com.example.applicationservice.application.ApplicationRepository;
import com.example.applicationservice.application.enums.ApplicationStatus;
import com.example.applicationservice.application.web.ApplicationResponse;
import com.example.applicationservice.application.web.CreateApplicationRequest;
import com.example.applicationservice.kafka.ApplicationEventPublisher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final Map<ApplicationStatus, List<ApplicationStatus>> statusTransition = Map.of(
            ApplicationStatus.NEW, List.of(ApplicationStatus.SCORING_IN_PROGRESS, ApplicationStatus.FAILED),
            ApplicationStatus.SCORING_IN_PROGRESS, List.of(ApplicationStatus.SCORING_APPROVED, ApplicationStatus.SCORING_REJECTED, ApplicationStatus.FAILED),
            ApplicationStatus.SCORING_APPROVED, List.of(ApplicationStatus.APPROVED, ApplicationStatus.FAILED),
            ApplicationStatus.APPROVED, List.of(ApplicationStatus.ISSUED, ApplicationStatus.FAILED)
    );
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final ApplicationEventPublisher eventPublisher;

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

        eventPublisher.publishApplicationCreated(
                applicationMapper.toApplicationCreatedEvent(savedEntity)
        );

        return applicationMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        return applicationMapper.toResponse(applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка с ID " + id + " не найдена")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    @Override
    public ApplicationResponse updateApplicationStatus(Long id, ApplicationStatus status) {
        ApplicationEntity entity = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка с ID " + id + " не найдена"));

        ApplicationStatus currentStatus = entity.getStatus();

        List<ApplicationStatus> nextStatuses = statusTransition.getOrDefault(
                currentStatus,
                List.of()
        );

        if(nextStatuses.contains(status)){
            entity.setStatus(status);
        }else {
            throw new IllegalStateException(
                    "Невозможно перейти из статуса " + currentStatus + " в статус " + status
            );
        }

        applicationRepository.save(entity);

        return applicationMapper.toResponse(entity);
    }
}

package com.example.applicationservice.application.service;

import com.example.applicationservice.application.ApplicationEntity;
import com.example.applicationservice.application.ApplicationMapper;
import com.example.applicationservice.application.ApplicationRepository;
import com.example.applicationservice.application.enums.ApplicationStatus;
import com.example.applicationservice.application.web.ApplicationResponse;
import com.example.applicationservice.application.web.CreateApplicationRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;

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
        applicationRepository.save(entity);

        return applicationMapper.toResponse(entity);
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
}

package com.example.applicationservice.application.service;

import com.example.applicationservice.application.web.ApplicationResponse;
import com.example.applicationservice.application.web.CreateApplicationRequest;
import com.example.enums.ApplicationStatus;
import com.example.event.ScoringCompletedEvent;

import java.util.List;

public interface ApplicationService {

    /**
     * Создание заявки на кредит
     * @param request - Данные для создания заявки
     * @return ApplicationResponse - Созданная заявка
     */
    ApplicationResponse createApplication(CreateApplicationRequest request);

    /**
     * Получение заявки по ID
     * @param id - ID заявки
     * @return ApplicationResponse - Найденная заявка
     */
    ApplicationResponse getApplicationById(Long id);

    /**
     * Получение всех заявок
     * @return ist<ApplicationResponse> - Список заявок
     */
    List<ApplicationResponse> getAllApplications();

    /**
     * Обновление статуса заявки
     * @param id - ID заявки
     * @param status - Новый статус заявки
     * @return ApplicationResponse - Обновленная заявка
     */
    ApplicationResponse updateApplicationStatus(Long id, ApplicationStatus status);

    /**
     * Обновление статуса заявки после получения результата скоринга
     * @param event - Событие из kafka
     */
    void processScoringResult(ScoringCompletedEvent event);

    /**
     * Выдача кредита
     * @param id - Id заявки
     * @return ApplicationResponse - Выданная заявка
     */
    ApplicationResponse issueApplication(Long id);
}

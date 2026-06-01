package com.example.applicationservice.saga;

import com.example.applicationservice.application.service.ApplicationService;
import com.example.applicationservice.notificationServiceComponent.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoanSagaOrchestrator {

    private final NotificationService notificationService;
    private final ApplicationService applicationService;
    private final SagaService sagaService;

    public void issueWithNotification(Long id) {
        boolean issued = false;
        UUID sagaId = UUID.randomUUID();

        try {
            sagaService.log(sagaId, id, SagaStep.ISSUE, SagaStepStatus.STARTED, "Issue started");
            applicationService.issueApplication(id);
            issued = true;
            sagaService.log(sagaId, id, SagaStep.ISSUE, SagaStepStatus.SUCCESS, "Issue completed");

            sagaService.log(sagaId, id, SagaStep.NOTIFICATION, SagaStepStatus.STARTED, "Notification started");
            notificationService.sendIssueNotification(id);
            sagaService.log(sagaId, id, SagaStep.NOTIFICATION, SagaStepStatus.SUCCESS, "Notification sent");
        } catch (Exception e) {
            if (issued) {
                sagaService.log(sagaId, id, SagaStep.NOTIFICATION, SagaStepStatus.FAILED, e.getMessage());

                sagaService.log(sagaId, id, SagaStep.COMPENSATION_CANCEL_ISSUE, SagaStepStatus.STARTED, "Cancel issue compensation started");
                try {
                    applicationService.cancelIssuedApplication(
                            id,
                            "Failed to send notification: " + e.getMessage()
                    );
                    sagaService.log(sagaId, id, SagaStep.COMPENSATION_CANCEL_ISSUE, SagaStepStatus.SUCCESS, "Cancel issue compensation completed");
                } catch (Exception compensationException) {
                    sagaService.log(sagaId, id, SagaStep.COMPENSATION_CANCEL_ISSUE, SagaStepStatus.FAILED, compensationException.getMessage());
                    throw new RuntimeException(
                            "Saga failed for application ID: " + id + ". Issue compensation failed.",
                            compensationException
                    );
                }
                throw new RuntimeException(
                        "Saga failed for application ID: " + id + ". Rolled back issued application.",
                        e
                );
            }

            sagaService.log(sagaId, id, SagaStep.ISSUE, SagaStepStatus.FAILED, e.getMessage());

            throw new RuntimeException(
                    "Saga failed for application ID: " + id + ". Issue was not completed, compensation is not required.",
                    e
            );
        }
    }
}

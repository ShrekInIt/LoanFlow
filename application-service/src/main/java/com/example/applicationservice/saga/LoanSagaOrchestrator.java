package com.example.applicationservice.saga;

import com.example.applicationservice.application.service.ApplicationService;
import com.example.applicationservice.notificationServiceComponent.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanSagaOrchestrator {

    private final NotificationService notificationService;
    private final ApplicationService applicationService;

    public void issueWithNotification(Long id) {
        boolean issued = false;

        try {
            applicationService.issueApplication(id);
            issued = true;

            notificationService.sendIssueNotification(id);
        } catch (Exception e) {
            if (issued) {
                applicationService.cancelIssuedApplication(
                        id,
                        "Failed to send notification: " + e.getMessage()
                );

                throw new RuntimeException(
                        "Saga failed for application ID: " + id + ". Rolled back issued application.",
                        e
                );
            }

            throw new RuntimeException(
                    "Saga failed for application ID: " + id + ". Issue was not completed, compensation is not required.",
                    e
            );
        }
    }
}

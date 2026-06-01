package com.example.applicationservice.service;

import com.example.applicationservice.application.service.ApplicationService;
import com.example.applicationservice.notificationServiceComponent.NotificationService;
import com.example.applicationservice.saga.LoanSagaOrchestrator;
import com.example.applicationservice.saga.SagaService;
import com.example.applicationservice.saga.SagaStep;
import com.example.applicationservice.saga.SagaStepStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanSagaOrchestratorTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private SagaService sagaService;

    @Test
    void issueWithNotification_whenSuccessful_shouldIssueAndNotify() {
        LoanSagaOrchestrator orchestrator = new LoanSagaOrchestrator(
                notificationService,
                applicationService,
                sagaService
        );

        orchestrator.issueWithNotification(1L);

        InOrder inOrder = inOrder(sagaService, applicationService, notificationService);
        verifyLog(inOrder, SagaStep.ISSUE, SagaStepStatus.STARTED, "Issue started");
        inOrder.verify(applicationService).issueApplication(1L);
        verifyLog(inOrder, SagaStep.ISSUE, SagaStepStatus.SUCCESS, "Issue completed");
        verifyLog(inOrder, SagaStep.NOTIFICATION, SagaStepStatus.STARTED, "Notification started");
        inOrder.verify(notificationService).sendIssueNotification(1L);
        verifyLog(inOrder, SagaStep.NOTIFICATION, SagaStepStatus.SUCCESS, "Notification sent");
        verify(applicationService, never()).cancelIssuedApplication(any(), any());
    }

    @Test
    void issueWithNotification_whenIssueFails_shouldNotCompensate() {
        LoanSagaOrchestrator orchestrator = new LoanSagaOrchestrator(
                notificationService,
                applicationService,
                sagaService
        );
        doThrow(new IllegalStateException("Issue failed"))
                .when(applicationService)
                .issueApplication(1L);

        assertThatThrownBy(() -> orchestrator.issueWithNotification(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("compensation is not required");

        verify(sagaService).log(any(UUID.class), eq(1L), eq(SagaStep.ISSUE), eq(SagaStepStatus.FAILED), eq("Issue failed"));
        verify(notificationService, never()).sendIssueNotification(any());
        verify(applicationService, never()).cancelIssuedApplication(any(), any());
    }

    @Test
    void issueWithNotification_whenNotificationFails_shouldCancelIssue() {
        LoanSagaOrchestrator orchestrator = new LoanSagaOrchestrator(
                notificationService,
                applicationService,
                sagaService
        );
        doThrow(new IllegalStateException("Notification unavailable"))
                .when(notificationService)
                .sendIssueNotification(1L);

        assertThatThrownBy(() -> orchestrator.issueWithNotification(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rolled back issued application");

        verify(applicationService).cancelIssuedApplication(
                1L,
                "Failed to send notification: Notification unavailable"
        );
        verify(sagaService).log(
                any(UUID.class),
                eq(1L),
                eq(SagaStep.NOTIFICATION),
                eq(SagaStepStatus.FAILED),
                eq("Notification unavailable")
        );
        verify(sagaService).log(
                any(UUID.class),
                eq(1L),
                eq(SagaStep.COMPENSATION_CANCEL_ISSUE),
                eq(SagaStepStatus.SUCCESS),
                eq("Cancel issue compensation completed")
        );
    }

    @Test
    void issueWithNotification_whenCompensationFails_shouldLogFailure() {
        LoanSagaOrchestrator orchestrator = new LoanSagaOrchestrator(
                notificationService,
                applicationService,
                sagaService
        );
        doThrow(new IllegalStateException("Notification unavailable"))
                .when(notificationService)
                .sendIssueNotification(1L);
        doThrow(new IllegalStateException("Cancellation unavailable"))
                .when(applicationService)
                .cancelIssuedApplication(anyLong(), anyString());

        assertThatThrownBy(() -> orchestrator.issueWithNotification(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Issue compensation failed");

        verify(sagaService).log(
                any(UUID.class),
                eq(1L),
                eq(SagaStep.COMPENSATION_CANCEL_ISSUE),
                eq(SagaStepStatus.FAILED),
                eq("Cancellation unavailable")
        );
    }

    private void verifyLog(InOrder inOrder, SagaStep step, SagaStepStatus status, String message) {
        inOrder.verify(sagaService).log(any(UUID.class), eq(1L), eq(step), eq(status), eq(message));
    }
}

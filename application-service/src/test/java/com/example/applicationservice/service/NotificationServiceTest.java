package com.example.applicationservice.service;

import com.example.applicationservice.notificationServiceComponent.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();

    @Test
    void sendIssueNotification_whenFailureDisabled_shouldComplete() {
        ReflectionTestUtils.setField(notificationService, "flag", false);

        assertThatNoException().isThrownBy(() -> notificationService.sendIssueNotification(1L));
    }

    @Test
    void sendIssueNotification_whenFailureEnabled_shouldThrow() {
        ReflectionTestUtils.setField(notificationService, "flag", true);

        assertThatThrownBy(() -> notificationService.sendIssueNotification(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("1");
    }
}

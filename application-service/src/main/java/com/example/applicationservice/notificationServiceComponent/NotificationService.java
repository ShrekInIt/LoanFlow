package com.example.applicationservice.notificationServiceComponent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    @Value("${notification.fail-enabled}")
    private boolean flag;

    public void sendIssueNotification(Long applicationId){
        log.info("Sending issue notification for application ID: {}", applicationId);

        if (flag) {
            throw new RuntimeException("Simulated notification failure for application ID: " + applicationId);
        }

        log.info("Issue notification sent successfully for application ID: {}", applicationId);
    }
}

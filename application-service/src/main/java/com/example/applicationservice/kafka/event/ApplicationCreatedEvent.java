package com.example.applicationservice.kafka.event;

import com.example.applicationservice.application.enums.ApplicationStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ApplicationCreatedEvent(
        String eventId,
        Long applicationId,
        String fullName,
        BigDecimal salary,
        BigDecimal creditAmount,
        String creditPurpose,
        ApplicationStatus status,
        LocalDateTime createdAt
) {}

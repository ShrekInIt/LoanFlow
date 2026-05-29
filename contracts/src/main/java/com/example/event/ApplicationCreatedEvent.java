package com.example.event;

import com.example.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

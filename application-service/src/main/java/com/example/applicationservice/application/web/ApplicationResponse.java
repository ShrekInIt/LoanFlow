package com.example.applicationservice.application.web;

import com.example.enums.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ApplicationResponse(
        Long id,
        String fullName,
        BigDecimal salary,
        BigDecimal creditAmount,
        String creditPurpose,
        ApplicationStatus status,
        @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
        LocalDateTime updatedAt
) {}

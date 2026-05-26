package com.example.applicationservice.application.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateApplicationRequest(
        @NotNull
        String fullName,

        @NotNull
        @Positive
        BigDecimal salary,

        @NotNull
        @Positive
        BigDecimal creditAmount,

        @NotNull
        @Size(min=10)
        String creditPurpose
) {}

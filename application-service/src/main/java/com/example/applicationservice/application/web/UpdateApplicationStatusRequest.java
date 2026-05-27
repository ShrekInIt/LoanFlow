package com.example.applicationservice.application.web;

import com.example.applicationservice.application.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
        @NotNull ApplicationStatus status
) {}

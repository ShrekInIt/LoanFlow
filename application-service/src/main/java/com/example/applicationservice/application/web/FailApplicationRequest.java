package com.example.applicationservice.application.web;

import jakarta.validation.constraints.NotBlank;

public record FailApplicationRequest(@NotBlank String reason) {
}

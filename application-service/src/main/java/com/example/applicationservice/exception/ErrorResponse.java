package com.example.applicationservice.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponse(
    String message,

    String detailMessage,

    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    LocalDateTime errorTime
){}

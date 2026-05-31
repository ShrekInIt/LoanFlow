package com.example.event;

import java.time.LocalDateTime;

public record IssueCancelledEvent(
        String eventId,
        Long applicationId,
        String reason,
        LocalDateTime canceledAt
) {}

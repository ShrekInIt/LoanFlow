package com.example.event;

import java.time.LocalDateTime;

public record ScoringCompletedEvent(
        String eventId,
        String sourceEventId,
        Long applicationId,
        boolean approved,
        String reason,
        LocalDateTime scoredAt
) {}

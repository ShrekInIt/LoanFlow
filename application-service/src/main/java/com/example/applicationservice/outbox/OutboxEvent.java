package com.example.applicationservice.outbox;

import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String aggregateId,
        String aggregateType,
        String eventType,
        String topic,
        String payload,
        OutboxEventStatus status
) {}

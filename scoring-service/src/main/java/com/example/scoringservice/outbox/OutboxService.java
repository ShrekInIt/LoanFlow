package com.example.scoringservice.outbox;

import com.example.event.ScoringCompletedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    @Value("${topics.scoring.completed}")
    private String topic;
    private static final String EVENT_TYPE_SCORING_COMPLETED = "APPLICATION";
    private static final String EVENT_TYPE_APPLICATION_CREATED = "SCORING_COMPLETED";
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public void saveScoringCompletedEvent(ScoringCompletedEvent event){
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize ApplicationCreatedEvent", ex);
        }

        OutboxEventEntity eventOutbox = OutboxEventEntity.builder()
                .aggregateType(EVENT_TYPE_SCORING_COMPLETED)
                .aggregateId(event.applicationId().toString())
                .eventType(EVENT_TYPE_APPLICATION_CREATED)
                .topic(topic)
                .payload(payload)
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .maxRetries(5)
                .build();

        repository.save(eventOutbox);
    }

}

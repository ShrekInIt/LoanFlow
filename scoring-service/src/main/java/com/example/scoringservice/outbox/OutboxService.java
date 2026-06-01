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
    private static final String AGGREGATE_TYPE_APPLICATION = "APPLICATION";
    private static final String EVENT_TYPE_SCORING_COMPLETED = "SCORING_COMPLETED";
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public void saveScoringCompletedEvent(ScoringCompletedEvent event){
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize ScoringCompletedEvent", ex);
        }

        OutboxEventEntity eventOutbox = OutboxEventEntity.builder()
                .aggregateType(AGGREGATE_TYPE_APPLICATION)
                .aggregateId(event.applicationId().toString())
                .eventType(EVENT_TYPE_SCORING_COMPLETED)
                .topic(topic)
                .payload(payload)
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .maxRetries(5)
                .build();

        repository.save(eventOutbox);
    }

}

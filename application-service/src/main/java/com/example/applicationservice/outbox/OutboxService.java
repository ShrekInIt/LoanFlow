package com.example.applicationservice.outbox;

import com.example.applicationservice.application.ApplicationEntity;
import com.example.applicationservice.application.ApplicationMapper;
import com.example.event.ApplicationCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    @Value("${topics.application.created}")
    private String topic;
    private static final String AGGREGATE_TYPE_APPLICATION = "APPLICATION";
    private static final String EVENT_TYPE_APPLICATION_CREATED = "APPLICATION_CREATED";
    private final OutboxEventRepository repository;
    private final ApplicationMapper applicationMapper;
    private final ObjectMapper objectMapper;

    public void saveApplicationCreatedEvent(ApplicationEntity entity){
        ApplicationCreatedEvent payloadEvent = applicationMapper.toApplicationCreatedEvent(entity);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadEvent);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize ApplicationCreatedEvent", ex);
        }

        OutboxEventEntity event = OutboxEventEntity.builder()
                .aggregateType(AGGREGATE_TYPE_APPLICATION)
                .aggregateId(entity.getId().toString())
                .eventType(EVENT_TYPE_APPLICATION_CREATED)
                .topic(topic)
                .payload(payload)
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .maxRetries(5)
                .build();

        repository.save(event);
    }

}

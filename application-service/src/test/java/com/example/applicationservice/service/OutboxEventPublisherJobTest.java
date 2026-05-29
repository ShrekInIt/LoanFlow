package com.example.applicationservice.service;

import com.example.applicationservice.kafka.ApplicationEventPublisher;
import com.example.applicationservice.outbox.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxEventPublisherJobTest {
    @Mock
    OutboxEventRepository outboxEventRepository;

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Mock
    OutboxEventMapper mapper;

    @InjectMocks
    OutboxEventPublisherJob outboxEventPublisherJob;

    @Test
    void publishEvent_whenKafkaSuccess_shouldMarkAsPublished() throws Exception {
        OutboxEventEntity entity = getOutboxEntity();
        OutboxEvent event = getOutboxEvent(entity);

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(entity));

        when(mapper.toOutboxEvent(entity))
                .thenReturn(event);

        outboxEventPublisherJob.publishEvent();

        assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(entity.getPublishedAt()).isNotNull();
        assertThat(entity.getErrorMessage()).isNull();

        verify(applicationEventPublisher).publish(event);
    }

    @Test
    void publishEvent_whenKafkaFailed_shouldIncrementRetryCount() throws Exception {
        OutboxEventEntity entity = getOutboxEntity();
        OutboxEvent event = getOutboxEvent(entity);

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(entity));

        when(mapper.toOutboxEvent(entity))
                .thenReturn(event);

        doThrow(new RuntimeException("Kafka unavailable"))
                .when(applicationEventPublisher)
                .publish(event);

        outboxEventPublisherJob.publishEvent();

        assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(entity.getRetryCount()).isEqualTo(1);
        assertThat(entity.getErrorMessage()).contains("Kafka unavailable");
    }

    private OutboxEventEntity getOutboxEntity() {
        return OutboxEventEntity.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .aggregateType("APPLICATION")
                .aggregateId("1")
                .eventType("APPLICATION_CREATED")
                .topic("application.created")
                .payload("""
                    {
                      "eventId": "application-created-event-id",
                      "applicationId": 1,
                      "fullName": "John Boy Boy",
                      "salary": 100000,
                      "creditAmount": 50000,
                      "creditPurpose": "Buying family car",
                      "status": "SCORING_IN_PROGRESS",
                      "createdAt": "2026-05-28T12:00:00"
                    }
                    """)
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .maxRetries(5)
                .errorMessage(null)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .updatedAt(LocalDateTime.now().minusMinutes(1))
                .publishedAt(null)
                .build();
    }

    private OutboxEvent getOutboxEvent(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateId(),
                entity.getAggregateType(),
                entity.getEventType(),
                entity.getTopic(),
                entity.getPayload(),
                entity.getStatus()
        );
    }
}

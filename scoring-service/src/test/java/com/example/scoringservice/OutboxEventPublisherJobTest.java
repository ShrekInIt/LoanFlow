package com.example.scoringservice;

import com.example.scoringservice.kafka.ScoringEventPublisher;
import com.example.scoringservice.metrics.OutboxMetrics;
import com.example.scoringservice.outbox.*;
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
class OutboxEventPublisherJobTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ScoringEventPublisher scoringEventPublisher;

    @Mock
    private OutboxEventMapper mapper;

    @Mock
    private OutboxMetrics outboxMetrics;

    @InjectMocks
    private OutboxEventPublisherJob job;

    @Test
    void publishEvent_whenKafkaSuccess_shouldMarkAsPublished() throws Exception {
        OutboxEventEntity entity = getOutboxEntity();
        OutboxEvent event = getOutboxEvent(entity);

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(entity));

        when(mapper.toOutboxEvent(entity))
                .thenReturn(event);

        job.publishEvent();

        assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(entity.getPublishedAt()).isNotNull();
        assertThat(entity.getErrorMessage()).isNull();

        verify(scoringEventPublisher).publish(event);
        verify(outboxMetrics).incrementOutboxPublished();
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
                .when(scoringEventPublisher)
                .publish(event);

        job.publishEvent();

        assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(entity.getRetryCount()).isEqualTo(1);
        assertThat(entity.getErrorMessage()).contains("Kafka unavailable");

        verify(scoringEventPublisher).publish(event);
        verify(outboxMetrics).incrementOutboxFailed();
    }

    @Test
    void publishEvent_whenMaxRetriesReached_shouldMarkAsFailed() throws Exception {
        OutboxEventEntity entity = getOutboxEntity();
        entity.setRetryCount(4);
        entity.setMaxRetries(5);

        OutboxEvent event = getOutboxEvent(entity);

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(entity));

        when(mapper.toOutboxEvent(entity))
                .thenReturn(event);

        doThrow(new RuntimeException("Kafka unavailable"))
                .when(scoringEventPublisher)
                .publish(event);

        job.publishEvent();

        assertThat(entity.getRetryCount()).isEqualTo(5);
        assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(entity.getErrorMessage()).contains("Kafka unavailable");

        verify(scoringEventPublisher).publish(event);
        verify(outboxMetrics).incrementOutboxFailed();
    }

    private OutboxEventEntity getOutboxEntity() {
        return OutboxEventEntity.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .aggregateType("APPLICATION")
                .aggregateId("1")
                .eventType("SCORING_COMPLETED")
                .topic("scoring.completed")
                .payload("{\"eventId\":\"scoring-event-id\",\"applicationId\":1}")
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

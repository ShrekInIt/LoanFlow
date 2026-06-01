package com.example.scoringservice;

import com.example.enums.ApplicationStatus;
import com.example.event.ApplicationCreatedEvent;
import com.example.event.ScoringCompletedEvent;
import com.example.scoringservice.idempotency.ProcessedEventService;
import com.example.scoringservice.kafka.ApplicationCreatedConsumer;
import com.example.scoringservice.outbox.OutboxService;
import com.example.scoringservice.scoring.ScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationCreatedConsumerTest {
    @Mock
    private ScoringService scoringService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ProcessedEventService processedEventService;

    @InjectMocks
    private ApplicationCreatedConsumer consumer;

    @Test
    void consume_whenEventNotProcessed_shouldScorePublishAndMarkProcessed() {
        ApplicationCreatedEvent event = getApplicationCreatedEvent();
        ScoringCompletedEvent completedEvent = getScoringCompletedEvent(event);

        when(processedEventService.isProcessed(
                eq(event.eventId()),
                anyString()
        )).thenReturn(false);

        when(scoringService.scoring(event))
                .thenReturn(completedEvent);

        consumer.consume(event);

        verify(processedEventService).isProcessed(eq(event.eventId()), anyString());
        verify(scoringService).scoring(event);
        verify(outboxService).saveScoringCompletedEvent(completedEvent);
        verify(processedEventService).markAsProcessed(eq(event.eventId()), anyString());
    }

    @Test
    void consume_whenEventAlreadyProcessed_shouldSkip() {
        ApplicationCreatedEvent event = getApplicationCreatedEvent();

        when(processedEventService.isProcessed(
                eq(event.eventId()),
                anyString()
        )).thenReturn(true);

        consumer.consume(event);

        verify(processedEventService).isProcessed(eq(event.eventId()), anyString());
        verify(scoringService, never()).scoring(any());
        verify(outboxService, never()).saveScoringCompletedEvent(any());
        verify(processedEventService, never()).markAsProcessed(anyString(), anyString());
    }

    @Test
    void consume_whenOutboxSaveFails_shouldNotMarkEventAsProcessed() {
        ApplicationCreatedEvent event = getApplicationCreatedEvent();
        ScoringCompletedEvent completedEvent = getScoringCompletedEvent(event);

        when(processedEventService.isProcessed(eq(event.eventId()), anyString())).thenReturn(false);
        when(scoringService.scoring(event)).thenReturn(completedEvent);
        doThrow(new IllegalStateException("Outbox save failed"))
                .when(outboxService)
                .saveScoringCompletedEvent(completedEvent);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Outbox save failed");

        verify(processedEventService, never()).markAsProcessed(anyString(), anyString());
    }

    private ApplicationCreatedEvent getApplicationCreatedEvent() {
        return new ApplicationCreatedEvent(
                "application-created-event-id",
                1L,
                "John Boy Boy",
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(500_000),
                "Buying family car",
                ApplicationStatus.SCORING_IN_PROGRESS,
                LocalDateTime.now()
        );
    }

    private ScoringCompletedEvent getScoringCompletedEvent(ApplicationCreatedEvent event) {
        return new ScoringCompletedEvent(
                "scoring-completed-event-id",
                event.eventId(),
                event.applicationId(),
                true,
                "Approved",
                LocalDateTime.now()
        );
    }
}

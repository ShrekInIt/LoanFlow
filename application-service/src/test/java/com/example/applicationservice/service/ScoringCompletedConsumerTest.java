package com.example.applicationservice.service;

import com.example.applicationservice.application.service.ApplicationService;
import com.example.applicationservice.idempotency.ProcessedEventService;
import com.example.applicationservice.kafka.ScoringCompletedConsumer;
import com.example.event.ScoringCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringCompletedConsumerTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ProcessedEventService processedEventService;

    @InjectMocks
    private ScoringCompletedConsumer consumer;

    @Test
    void consume_whenEventIsNew_shouldProcessScoringResult() {
        ScoringCompletedEvent event = getEvent();
        when(processedEventService.isProcessed(eq(event.eventId()), anyString())).thenReturn(false);

        consumer.consume(event);

        verify(applicationService).processScoringResult(event);
        verify(processedEventService).markAsProcessed(eq(event.eventId()), anyString());
    }

    @Test
    void consume_whenEventWasProcessed_shouldSkip() {
        ScoringCompletedEvent event = getEvent();
        when(processedEventService.isProcessed(eq(event.eventId()), anyString())).thenReturn(true);

        consumer.consume(event);

        verify(applicationService, never()).processScoringResult(any());
        verify(processedEventService, never()).markAsProcessed(anyString(), anyString());
    }

    @Test
    void consume_whenProcessingFails_shouldNotMarkEventAsProcessed() {
        ScoringCompletedEvent event = getEvent();
        when(processedEventService.isProcessed(eq(event.eventId()), anyString())).thenReturn(false);
        doThrow(new IllegalStateException("Processing failed"))
                .when(applicationService)
                .processScoringResult(event);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Processing failed");

        verify(processedEventService, never()).markAsProcessed(anyString(), anyString());
    }

    private ScoringCompletedEvent getEvent() {
        return new ScoringCompletedEvent(
                "scoring-event-id",
                "source-event-id",
                1L,
                true,
                "Approved",
                LocalDateTime.now()
        );
    }
}

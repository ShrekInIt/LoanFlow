package com.example.scoringservice;

import com.example.event.ScoringCompletedEvent;
import com.example.scoringservice.outbox.OutboxEventEntity;
import com.example.scoringservice.outbox.OutboxEventRepository;
import com.example.scoringservice.outbox.OutboxEventStatus;
import com.example.scoringservice.outbox.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void saveScoringCompletedEvent_success() throws Exception {
        ReflectionTestUtils.setField(outboxService, "topic", "scoring.completed");

        ScoringCompletedEvent event = getScoringCompletedEvent();

        when(objectMapper.writeValueAsString(event))
                .thenReturn("{\"eventId\":\"scoring-event-id\",\"applicationId\":1}");

        outboxService.saveScoringCompletedEvent(event);

        ArgumentCaptor<OutboxEventEntity> captor =
                ArgumentCaptor.forClass(OutboxEventEntity.class);

        verify(repository).save(captor.capture());

        OutboxEventEntity saved = captor.getValue();

        assertThat(saved.getAggregateType()).isEqualTo("APPLICATION");
        assertThat(saved.getAggregateId()).isEqualTo("1");
        assertThat(saved.getEventType()).isEqualTo("SCORING_COMPLETED");
        assertThat(saved.getTopic()).isEqualTo("scoring.completed");
        assertThat(saved.getPayload()).isEqualTo("{\"eventId\":\"scoring-event-id\",\"applicationId\":1}");
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getMaxRetries()).isEqualTo(5);
    }

    @Test
    void saveScoringCompletedEvent_whenSerializationFailed_shouldThrowException() throws Exception {
        ReflectionTestUtils.setField(outboxService, "topic", "scoring.completed");

        ScoringCompletedEvent event = getScoringCompletedEvent();

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> outboxService.saveScoringCompletedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize");

        verify(repository, never()).save(any());
    }

    private ScoringCompletedEvent getScoringCompletedEvent() {
        return new ScoringCompletedEvent(
                "scoring-event-id",
                "application-created-event-id",
                1L,
                true,
                "Approved",
                LocalDateTime.now()
        );
    }
}

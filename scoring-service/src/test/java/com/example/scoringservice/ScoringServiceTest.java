package com.example.scoringservice;

import com.example.enums.ApplicationStatus;
import com.example.event.ApplicationCreatedEvent;
import com.example.event.ScoringCompletedEvent;
import com.example.scoringservice.metrics.ScoringMetrics;
import com.example.scoringservice.scoring.ScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private ScoringMetrics scoringMetrics;

    @InjectMocks
    private ScoringService scoringService;

    @Test
    void scoring_whenSalaryAndAmountAreAtLimits_shouldApprove() {
        ScoringCompletedEvent result = scoringService.scoring(event("50000", "500000"));

        assertThat(result.approved()).isTrue();
        assertThat(result.reason()).isEqualTo("Approved");
        assertThat(result.sourceEventId()).isEqualTo("application-created-event-id");
        assertThat(result.applicationId()).isEqualTo(1L);
        assertThat(result.eventId()).isNotBlank();
        assertThat(result.scoredAt()).isNotNull();
        verify(scoringMetrics).incrementScoringRequests();
        verify(scoringMetrics).incrementScoringApproved();
    }

    @Test
    void scoring_whenSalaryIsBelowLimit_shouldReject() {
        ScoringCompletedEvent result = scoringService.scoring(event("49999.99", "100000"));

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).contains("insufficient salary");
        verify(scoringMetrics).incrementScoringRequests();
        verify(scoringMetrics).incrementScoringRejected();
    }

    @Test
    void scoring_whenAmountExceedsTenSalaries_shouldReject() {
        ScoringCompletedEvent result = scoringService.scoring(event("50000", "500000.01"));

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).contains("excessive credit amount");
        verify(scoringMetrics).incrementScoringRequests();
        verify(scoringMetrics).incrementScoringRejected();
    }

    private ApplicationCreatedEvent event(String salary, String amount) {
        return new ApplicationCreatedEvent(
                "application-created-event-id",
                1L,
                "John Boy Boy",
                new BigDecimal(salary),
                new BigDecimal(amount),
                "Buying family car",
                ApplicationStatus.SCORING_IN_PROGRESS,
                LocalDateTime.now()
        );
    }
}

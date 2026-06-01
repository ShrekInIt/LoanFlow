package com.example.scoringservice;

import com.example.scoringservice.metrics.OutboxMetrics;
import com.example.scoringservice.metrics.ScoringMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MetricsTest {

    @Test
    void scoringMetrics_shouldIncrementCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ScoringMetrics metrics = new ScoringMetrics(registry);

        metrics.incrementScoringRequests();
        metrics.incrementScoringApproved();
        metrics.incrementScoringRejected();

        assertThat(registry.counter("loan_scoring_requests_total").count()).isEqualTo(1);
        assertThat(registry.counter("loan_scoring_approved_total").count()).isEqualTo(1);
        assertThat(registry.counter("loan_scoring_rejected_total").count()).isEqualTo(1);
    }

    @Test
    void outboxMetrics_shouldIncrementCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OutboxMetrics metrics = new OutboxMetrics(registry);

        metrics.incrementOutboxPublished();
        metrics.incrementOutboxFailed();

        assertThat(registry.counter("loan_outbox_published_total").count()).isEqualTo(1);
        assertThat(registry.counter("loan_outbox_failed_total").count()).isEqualTo(1);
    }
}

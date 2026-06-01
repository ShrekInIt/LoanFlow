package com.example.applicationservice.service;

import com.example.applicationservice.metrics.ApplicationMetrics;
import com.example.applicationservice.metrics.OutboxMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MetricsTest {

    @Test
    void applicationMetrics_shouldIncrementCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApplicationMetrics metrics = new ApplicationMetrics(registry);

        metrics.incrementApplicationCreated();
        metrics.incrementScoringApproved();
        metrics.incrementScoringRejected();

        assertThat(registry.counter("loan_applications_created_total").count()).isEqualTo(1);
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

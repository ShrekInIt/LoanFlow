package com.example.applicationservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMetrics {

    private final Counter applicationsCreatedCounter;
    private final Counter scoringApprovedCounter;
    private final Counter scoringRejectedCounter;

    public ApplicationMetrics(MeterRegistry meterRegistry) {
        this.applicationsCreatedCounter = Counter.builder("loan_applications_created_total")
                .description("Total number of created loan applications")
                .register(meterRegistry);

        this.scoringApprovedCounter = Counter.builder("loan_scoring_approved_total")
                .description("Total number of applications approved by scoring")
                .register(meterRegistry);

        this.scoringRejectedCounter = Counter.builder("loan_scoring_rejected_total")
                .description("Total number of applications rejected by scoring")
                .register(meterRegistry);
    }

    public void incrementApplicationCreated() {
        applicationsCreatedCounter.increment();
    }

    public void incrementScoringApproved() {
        scoringApprovedCounter.increment();
    }

    public void incrementScoringRejected() {
        scoringRejectedCounter.increment();
    }
}
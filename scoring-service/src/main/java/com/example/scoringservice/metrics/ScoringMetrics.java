package com.example.scoringservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ScoringMetrics {

    private final Counter scoringRequestsCounter;
    private final Counter scoringApprovedCounter;
    private final Counter scoringRejectedCounter;

    public ScoringMetrics(MeterRegistry meterRegistry) {
        this.scoringRequestsCounter = Counter.builder("loan_scoring_requests_total")
                .description("Total number of scoring requests")
                .register(meterRegistry);

        this.scoringApprovedCounter = Counter.builder("loan_scoring_approved_total")
                .description("Total number of applications approved by scoring")
                .register(meterRegistry);

        this.scoringRejectedCounter = Counter.builder("loan_scoring_rejected_total")
                .description("Total number of applications rejected by scoring")
                .register(meterRegistry);
    }

    public void incrementScoringRequests() {
        scoringRequestsCounter.increment();
    }

    public void incrementScoringApproved() {
        scoringApprovedCounter.increment();
    }

    public void incrementScoringRejected() {
        scoringRejectedCounter.increment();
    }

}

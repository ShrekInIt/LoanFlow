package com.example.scoringservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {
    private final Counter loanOutboxPublishedCounter;
    private final Counter loanOutboxFailedCounter;

    public OutboxMetrics(MeterRegistry meterRegistry) {
        this.loanOutboxPublishedCounter = Counter.builder("loan_outbox_published_total")
                .description("Total number of created outbox applications")
                .register(meterRegistry);
        this.loanOutboxFailedCounter = Counter.builder("loan_outbox_failed_total")
                .description("Total number of created outbox applications failed")
                .register(meterRegistry);
    }

    public void incrementOutboxPublished(){
        loanOutboxPublishedCounter.increment();
    }

    public void incrementOutboxFailed(){
        loanOutboxFailedCounter.increment();
    }
}

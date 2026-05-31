package com.example.scoringservice.kafka;

import com.example.event.ApplicationCreatedEvent;
import com.example.event.ScoringCompletedEvent;
import com.example.scoringservice.idempotency.ProcessedEventService;
import com.example.scoringservice.outbox.OutboxService;
import com.example.scoringservice.scoring.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationCreatedConsumer {

    private final static String CONSUMER_NAME = "scoring-service.application-created-consumer";
    private final ScoringService scoringService;
    private final OutboxService outboxService;
    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "${topics.application.created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ApplicationCreatedEvent event) {
        log.info("Received application created message: {}", event);

        if(processedEventService.isProcessed(event.eventId(), CONSUMER_NAME)) {
            log.info("Event with ID {} has already been processed by consumer {}, skipping", event.applicationId(), CONSUMER_NAME);
            return;
        }

        ScoringCompletedEvent completedEvent = scoringService.scoring(event);
        outboxService.saveScoringCompletedEvent(completedEvent);
        processedEventService.markAsProcessed(event.eventId(), CONSUMER_NAME);
    }
}

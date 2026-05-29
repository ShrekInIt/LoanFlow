package com.example.scoringservice.kafka;

import com.example.event.ApplicationCreatedEvent;
import com.example.event.ScoringCompletedEvent;
import com.example.scoringservice.scoring.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationCreatedConsumer {

    private final ScoringService scoringService;
    private final ScoringEventPublisher scoringEventPublisher;

    @KafkaListener(
            topics = "${topics.application.created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ApplicationCreatedEvent event) throws Exception {
        log.info("Received application created message: {}", event);
        ScoringCompletedEvent completedEvent = scoringService.scoring(event);
        scoringEventPublisher.publish(completedEvent);
    }
}

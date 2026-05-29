package com.example.applicationservice.kafka;

import com.example.applicationservice.application.service.ApplicationService;
import com.example.event.ScoringCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScoringCompletedConsumer {
    private final ApplicationService applicationService;

    @KafkaListener(
            topics = "${topics.scoring.completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ScoringCompletedEvent event) {
        log.info("Received scoring completed message: {}", event);
        applicationService.processScoringResult(event);
    }
}

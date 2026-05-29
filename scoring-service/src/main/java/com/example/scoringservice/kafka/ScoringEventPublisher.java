package com.example.scoringservice.kafka;

import com.example.event.ScoringCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScoringEventPublisher {

    @Value("${topics.scoring.completed}")
    private String topic;

    private final KafkaTemplate<String, ScoringCompletedEvent> kafkaTemplate;

    public void publish(ScoringCompletedEvent event) {
        kafkaTemplate.send(topic, event.applicationId().toString(), event);
        log.info("Published scoring completed event to Kafka: key={}, payload={}", event.applicationId(), event);
    }
}

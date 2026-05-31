package com.example.scoringservice.kafka;

import com.example.scoringservice.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScoringEventPublisher {

    private final KafkaTemplate<String, String> outboxKafkaTemplate;

    public void publish(OutboxEvent event) throws Exception {
        log.info("Publishing outbox event: id={}, topic={}, aggregateId={}",
                event.id(), event.topic(), event.aggregateId());

        outboxKafkaTemplate
                .send(event.topic(), event.aggregateId(), event.payload())
                .get();
    }
}

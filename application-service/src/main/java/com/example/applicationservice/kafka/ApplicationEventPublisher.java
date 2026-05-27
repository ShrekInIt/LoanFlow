package com.example.applicationservice.kafka;

import com.example.applicationservice.kafka.event.ApplicationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationEventPublisher {

    @Value("${topics.application.created}")
    private String topic;

    private final KafkaTemplate<String, ApplicationCreatedEvent> kafkaTemplate;

    public void publishApplicationCreated(ApplicationCreatedEvent event){
        log.info("Publishing ApplicationCreatedEvent: applicationId={}, eventId={}",
                event.applicationId(), event.eventId());

        kafkaTemplate.send(topic, event.applicationId().toString(),event);
    }
}

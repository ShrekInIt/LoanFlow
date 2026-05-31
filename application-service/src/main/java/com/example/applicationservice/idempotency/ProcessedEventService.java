package com.example.applicationservice.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository repository;

    @Transactional(readOnly = true)
    public boolean isProcessed(String eventId, String consumerName) {
        if (repository.existsByEventIdAndConsumerName(eventId, consumerName)) {
            return true;
        }
        ProcessedEventEntity event = new ProcessedEventEntity();
        event.setEventId(eventId);
        event.setConsumerName(consumerName);
        repository.save(event);
        return false;
    }
}

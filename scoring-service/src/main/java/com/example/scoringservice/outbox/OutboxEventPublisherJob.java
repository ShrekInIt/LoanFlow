package com.example.scoringservice.outbox;

import com.example.scoringservice.kafka.ScoringEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisherJob {

    private final OutboxEventRepository outboxEventRepository;
    private final ScoringEventPublisher scoringEventPublisher;
    private final OutboxEventMapper mapper;

    @Transactional
    @Scheduled(fixedRate = 5000)
    public void publishEvent() {
        var pendingEvents = outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        for (var event : pendingEvents){
            try {
                scoringEventPublisher.publish(mapper.toOutboxEvent(event));
                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                event.setErrorMessage(null);
            }catch (Exception ex) {
                int retryCount = event.getRetryCount() + 1;
                event.setRetryCount(retryCount);
                event.setErrorMessage(ex.getMessage());

                if (retryCount >= event.getMaxRetries()) {
                    event.setStatus(OutboxEventStatus.FAILED);
                }
                log.error("Failed to publish outbox event: id={}, retryCount={}",
                        event.getId(), retryCount, ex);
            }
        }
    }
}

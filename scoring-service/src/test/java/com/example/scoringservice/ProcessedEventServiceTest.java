package com.example.scoringservice;

import com.example.scoringservice.idempotency.ProcessedEventEntity;
import com.example.scoringservice.idempotency.ProcessedEventRepository;
import com.example.scoringservice.idempotency.ProcessedEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedEventServiceTest {

    @Mock
    private ProcessedEventRepository repository;

    @InjectMocks
    private ProcessedEventService service;

    @Test
    void isProcessed_shouldDelegateToRepository() {
        when(repository.existsByEventIdAndConsumerName("event-id", "consumer")).thenReturn(true);

        assertThat(service.isProcessed("event-id", "consumer")).isTrue();
    }

    @Test
    void markAsProcessed_shouldSaveMarker() {
        service.markAsProcessed("event-id", "consumer");

        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("event-id");
        assertThat(captor.getValue().getConsumerName()).isEqualTo("consumer");
    }
}

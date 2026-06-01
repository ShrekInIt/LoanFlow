package com.example.applicationservice.service;

import com.example.applicationservice.idempotency.ProcessedEventEntity;
import com.example.applicationservice.idempotency.ProcessedEventRepository;
import com.example.applicationservice.idempotency.ProcessedEventService;
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
    void isProcessed_whenAlreadyStored_shouldReturnTrue() {
        when(repository.existsByEventIdAndConsumerName("event-id", "consumer")).thenReturn(true);

        assertThat(service.isProcessed("event-id", "consumer")).isTrue();

        verify(repository, never()).save(any());
    }

    @Test
    void isProcessed_whenNew_shouldReturnFalseWithoutStoringMarker() {
        when(repository.existsByEventIdAndConsumerName("event-id", "consumer")).thenReturn(false);

        assertThat(service.isProcessed("event-id", "consumer")).isFalse();

        verify(repository, never()).save(any());
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

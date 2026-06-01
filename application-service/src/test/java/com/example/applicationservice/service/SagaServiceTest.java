package com.example.applicationservice.service;

import com.example.applicationservice.saga.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SagaServiceTest {

    @Mock
    private SagaLogRepository sagaLogRepository;

    @InjectMocks
    private SagaService sagaService;

    @Test
    void log_shouldSaveSagaEntry() {
        UUID sagaId = UUID.randomUUID();

        sagaService.log(sagaId, 1L, SagaStep.ISSUE, SagaStepStatus.STARTED, "Issue started");

        ArgumentCaptor<SagaLogEntity> captor = ArgumentCaptor.forClass(SagaLogEntity.class);
        verify(sagaLogRepository).save(captor.capture());
        SagaLogEntity saved = captor.getValue();

        assertThat(saved.getSagaId()).isEqualTo(sagaId);
        assertThat(saved.getApplicationId()).isEqualTo(1L);
        assertThat(saved.getStep()).isEqualTo(SagaStep.ISSUE);
        assertThat(saved.getStatus()).isEqualTo(SagaStepStatus.STARTED);
        assertThat(saved.getMessage()).isEqualTo("Issue started");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}

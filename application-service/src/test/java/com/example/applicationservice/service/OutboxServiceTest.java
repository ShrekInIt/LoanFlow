package com.example.applicationservice.service;

import com.example.applicationservice.application.ApplicationEntity;
import com.example.applicationservice.application.ApplicationMapper;
import com.example.applicationservice.outbox.OutboxEventEntity;
import com.example.applicationservice.outbox.OutboxEventRepository;
import com.example.applicationservice.outbox.OutboxEventStatus;
import com.example.applicationservice.outbox.OutboxService;
import com.example.enums.ApplicationStatus;
import com.example.event.ApplicationCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxServiceTest {
    @Mock
    ApplicationMapper applicationMapper;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    OutboxEventRepository repository;

    @InjectMocks
    OutboxService outboxService;

    @Test
    void saveApplicationCreatedEvent_success() throws Exception{
        ReflectionTestUtils.setField(
                outboxService,
                "topic",
                "application.created"
        );
        ApplicationEntity entity = getEntity();
        ApplicationCreatedEvent event = getEvent(entity);

        when(applicationMapper.toApplicationCreatedEvent(entity))
                .thenReturn(event);

        when(objectMapper.writeValueAsString(event))
                .thenReturn("{\"applicationId\":1}");

        outboxService.saveApplicationCreatedEvent(entity);

        ArgumentCaptor<OutboxEventEntity> captor =
                ArgumentCaptor.forClass(OutboxEventEntity.class);

        verify(repository).save(captor.capture());

        OutboxEventEntity saved = captor.getValue();

        assertThat(saved.getAggregateType()).isEqualTo("APPLICATION");
        assertThat(saved.getAggregateId()).isEqualTo("1");
        assertThat(saved.getEventType()).isEqualTo("APPLICATION_CREATED");
        assertThat(saved.getTopic()).isEqualTo("application.created");
        assertThat(saved.getPayload()).isEqualTo("{\"applicationId\":1}");
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.NEW);
    }

    @Test
    void saveApplicationCreatedEvent_whenSerializationFailed_shouldThrowException() throws Exception {
        ApplicationEntity entity = getEntity();
        ApplicationCreatedEvent event = getEvent(entity);

        when(applicationMapper.toApplicationCreatedEvent(entity))
                .thenReturn(event);

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> outboxService.saveApplicationCreatedEvent(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize ApplicationCreatedEvent");

        verify(repository, never()).save(any());
    }

    private ApplicationEntity getEntity(){
        return ApplicationEntity.builder()
                .id(1L)
                .fullName("John Boy Boy")
                .salary(BigDecimal.valueOf(100000))
                .creditAmount(BigDecimal.valueOf(50000))
                .status(ApplicationStatus.SCORING_IN_PROGRESS)
                .creditPurpose("Buying family car")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0)
                .build();
    }

    private ApplicationCreatedEvent getEvent(ApplicationEntity entity){
        return new ApplicationCreatedEvent(
                "Yippso-02992",
                entity.getId(),
                entity.getFullName(),
                entity.getSalary(),
                entity.getCreditAmount(),
                entity.getCreditPurpose(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}

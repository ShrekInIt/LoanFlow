package com.example.applicationservice.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SagaService {

    private final SagaLogRepository sagaLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            UUID sagaId,
            Long applicationId,
            SagaStep step,
            SagaStepStatus status,
            String message
    ) {
        SagaLogEntity sagaLog = SagaLogEntity.builder()
                .sagaId(sagaId)
                .applicationId(applicationId)
                .step(step)
                .status(status)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        sagaLogRepository.save(sagaLog);
    }
}

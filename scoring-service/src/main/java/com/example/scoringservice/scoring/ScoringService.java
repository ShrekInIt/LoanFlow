package com.example.scoringservice.scoring;

import com.example.event.ApplicationCreatedEvent;
import com.example.event.ScoringCompletedEvent;
import com.example.scoringservice.metrics.ScoringMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ScoringMetrics scoringMetrics;

    public ScoringCompletedEvent scoring(ApplicationCreatedEvent event){
        boolean salaryEnough = event.salary().compareTo(BigDecimal.valueOf(50_000)) >= 0;
        boolean amountAcceptable = event.creditAmount()
                .compareTo(event.salary().multiply(BigDecimal.valueOf(10))) <= 0;
        scoringMetrics.incrementScoringRequests();
        boolean approved = salaryEnough && amountAcceptable;

        if(approved){
            scoringMetrics.incrementScoringApproved();
        }else{
            scoringMetrics.incrementScoringRejected();
        }

        return new ScoringCompletedEvent(
                UUID.randomUUID().toString(),
                event.eventId(),
                event.applicationId(),
                approved,
                approved ? "Approved" : "Rejected due to insufficient salary or excessive credit amount",
                LocalDateTime.now()
        );
    }
}

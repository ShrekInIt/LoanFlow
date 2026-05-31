package com.example.scoringservice.outbox;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OutboxEventMapper {
    OutboxEvent toOutboxEvent(OutboxEventEntity event);
}

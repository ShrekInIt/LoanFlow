package com.example.applicationservice.outbox;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OutboxEventMapper {
    OutboxEvent toOutboxEvent(OutboxEventEntity event);
}

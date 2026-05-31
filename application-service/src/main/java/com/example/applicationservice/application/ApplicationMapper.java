package com.example.applicationservice.application;

import com.example.applicationservice.application.web.ApplicationResponse;
import com.example.applicationservice.application.web.CreateApplicationRequest;
import com.example.event.ApplicationCreatedEvent;
import com.example.event.IssueCancelledEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    ApplicationResponse toResponse(ApplicationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "status", constant = "NEW")
    @Mapping(target = "version", constant = "0")
    ApplicationEntity toEntity(CreateApplicationRequest request);

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "applicationId", source = "id")
    ApplicationCreatedEvent toApplicationCreatedEvent(ApplicationEntity entity);

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "applicationId", source = "id")
    @Mapping(target = "reason", source = "failureReason")
    @Mapping(target = "canceledAt", expression = "java(java.time.LocalDateTime.now())")
    IssueCancelledEvent toIssueCanceledEvent(ApplicationEntity entity);
}

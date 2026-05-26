package com.example.applicationservice.application;

import com.example.applicationservice.application.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    boolean existsByFullNameAndStatus(String fullName, ApplicationStatus status);

    boolean existsByFullNameAndCreatedAtAfter(String fullName, LocalDateTime dateTime);
}

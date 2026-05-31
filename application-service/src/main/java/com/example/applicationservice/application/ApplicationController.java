package com.example.applicationservice.application;

import com.example.applicationservice.application.service.ApplicationService;
import com.example.applicationservice.application.web.ApplicationResponse;
import com.example.applicationservice.application.web.CreateApplicationRequest;
import com.example.applicationservice.application.web.FailApplicationRequest;
import com.example.applicationservice.application.web.UpdateApplicationStatusRequest;
import com.example.applicationservice.saga.LoanSagaOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService applicationService;
    private final LoanSagaOrchestrator loanSagaOrchestrator;

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @RequestBody @Valid CreateApplicationRequest request
    ) {
        log.info("Received request to create application");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(request));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<ApplicationResponse> failApplication(
            @PathVariable Long id,
            @RequestBody @Valid FailApplicationRequest request
    ) {
        log.info("Received request to fail application with id: {}, reason: {}", id, request);
        return ResponseEntity.ok(applicationService.failApplication(id, request.reason()));
    }

    @PostMapping("/{id}/cancel-issue")
    public ResponseEntity<ApplicationResponse> cancelIssuedApplication(
            @PathVariable Long id,
            @RequestBody @Valid FailApplicationRequest request
    ) {
        log.info("Received request to cancel issued application with id: {}, reason: {}", id, request);
        return ResponseEntity.ok(applicationService.cancelIssuedApplication(id, request.reason()));
    }

    @PostMapping("/{id}/issue-with-notification")
    public ResponseEntity<Void> issueApplicationWithNotification(
            @PathVariable Long id
    ) {
        log.info("Received request to issue application with id: {} with notification", id);
        loanSagaOrchestrator.issueWithNotification(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<ApplicationResponse> issueApplication(
            @PathVariable Long id
    ) {
        log.info("Received request to issue application with ID: {}", id);
        return ResponseEntity.ok(applicationService.issueApplication(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable Long id
    ) {
        log.info("Received request to get application by id={}", id);
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        log.info("Received request to get all applications");
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateApplicationStatusRequest request
    ){
        log.info("Received request to update application status, id={}, status={}", id, request.status());
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id,  request.status()));
    }
}

package com.enterprise.lms.controller;

import com.enterprise.lms.dto.CreateLoanApplicationRequest;
import com.enterprise.lms.dto.LoanApplicationDTO;
import com.enterprise.lms.entity.LoanApplication.ApplicationStatus;
import com.enterprise.lms.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    @PostMapping
    public ResponseEntity<LoanApplicationDTO> createApplication(
            @Valid @RequestBody CreateLoanApplicationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        UUID submittedBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanApplicationService.createApplication(request, submittedBy));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationDTO> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(loanApplicationService.getApplication(id));
    }

    @GetMapping("/by-number/{applicationNumber}")
    public ResponseEntity<LoanApplicationDTO> getApplicationByNumber(@PathVariable String applicationNumber) {
        return ResponseEntity.ok(loanApplicationService.getApplicationByNumber(applicationNumber));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LoanApplicationDTO>> getMyApplications(
            @RequestHeader(value = "X-User-Id") String userId) {
        return ResponseEntity.ok(loanApplicationService.getMyApplications(UUID.fromString(userId)));
    }

    @GetMapping()
    public ResponseEntity<List<LoanApplicationDTO>> getAllApplications() {
        return ResponseEntity.ok(loanApplicationService.getAllApplications());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoanApplicationDTO>> getApplicationsByStatus(
            @PathVariable ApplicationStatus status) {
        return ResponseEntity.ok(loanApplicationService.getApplicationsByStatus(status));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<LoanApplicationDTO> submitApplication(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        UUID submittedBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.ok(loanApplicationService.submitApplication(id, submittedBy));
    }

    @PatchMapping("/{id}/data")
    public ResponseEntity<LoanApplicationDTO> updateApplicationData(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(loanApplicationService.updateApplicationData(id, data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LoanApplicationDTO> updateStatus(
            @PathVariable UUID id,
            @RequestParam ApplicationStatus status,
            @RequestParam(required = false) String comments,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        UUID updatedBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.ok(loanApplicationService.updateStatus(id, status, comments, updatedBy));
    }
}

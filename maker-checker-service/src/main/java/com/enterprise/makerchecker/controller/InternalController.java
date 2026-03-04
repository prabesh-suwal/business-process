package com.enterprise.makerchecker.controller;

import com.enterprise.makerchecker.entity.ApprovalRequest;
import com.enterprise.makerchecker.enums.ApprovalStatus;
import com.enterprise.makerchecker.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Internal-only endpoint for inter-service communication.
 * Secured via shared secret token — NOT exposed through the gateway.
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final ApprovalRequestRepository approvalRepository;

    @Value("${internal.service.token}")
    private String internalServiceToken;

    /**
     * Verify that an approval exists and is in APPROVED status.
     * Called by the gateway before executing the approved request.
     */
    @Transactional(readOnly = true)
    @GetMapping("/approvals/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyApproval(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String token) {

        log.warn("id::::::::: {}", id);

        if (!internalServiceToken.equals(token)) {
            log.warn("Rejected internal verification request — invalid token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("valid", false, "reason", "Invalid service token"));
        }

        return approvalRepository.findById(id)
                .map(approval -> {
                    boolean valid = approval.getStatus() == ApprovalStatus.APPROVED;
                    Map<String, Object> result = Map.of(
                            "valid", valid,
                            "status", approval.getStatus().name(),
                            "serviceName", approval.getConfig().getServiceName(),
                            "requestPath", approval.getRequestPath(),
                            "httpMethod", approval.getHttpMethod());
                    log.info("Approval verification: id={} status={} valid={}",
                            id, approval.getStatus(), valid);
                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> {
                    log.warn("Approval verification failed: id={} not found", id);
                    return ResponseEntity.ok(Map.of("valid", false, "reason", "Not found"));
                });
    }
}

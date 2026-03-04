package com.enterprise.makerchecker.service;

import com.enterprise.makerchecker.dto.ApprovalActionRequest;
import com.enterprise.makerchecker.dto.ApprovalRequestResponse;
import com.enterprise.makerchecker.dto.CreateApprovalRequest;
import com.enterprise.makerchecker.entity.ApprovalAuditLog;
import com.enterprise.makerchecker.entity.ApprovalRequest;
import com.enterprise.makerchecker.entity.MakerCheckerConfig;
import com.enterprise.makerchecker.entity.SlaEscalationConfig;
import com.enterprise.makerchecker.enums.ApprovalStatus;
import com.enterprise.makerchecker.enums.AuditAction;
import com.enterprise.makerchecker.repository.ApprovalAuditLogRepository;
import com.enterprise.makerchecker.repository.ApprovalRequestRepository;
import com.enterprise.makerchecker.repository.SlaEscalationConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRepository;
    private final ApprovalAuditLogRepository auditLogRepository;
    private final SlaEscalationConfigRepository slaRepository;
    private final ConfigService configService;
    private final ExecutionService executionService;

    /**
     * Creates a new pending approval request (called by the gateway).
     */
    @Transactional
    public ApprovalRequestResponse createApproval(CreateApprovalRequest request) {
        MakerCheckerConfig config = configService.findMatchingConfig(
                request.getHttpMethod(), request.getRequestPath());

        if (config == null) {
            throw new IllegalArgumentException("No maker-checker config found for " +
                    request.getHttpMethod() + " " + request.getRequestPath());
        }

        // Calculate expiry from SLA config
        OffsetDateTime expiresAt = null;
        SlaEscalationConfig sla = slaRepository.findByConfigId(config.getId()).orElse(null);
        if (sla != null) {
            expiresAt = OffsetDateTime.now().plusHours(sla.getDeadlineHours());
        }

        ApprovalRequest approval = ApprovalRequest.builder()
                .config(config)
                .status(ApprovalStatus.PENDING)
                .httpMethod(request.getHttpMethod())
                .requestPath(request.getRequestPath())
                .requestBody(request.getRequestBody())
                .requestHeaders(request.getRequestHeaders())
                .queryParams(request.getQueryParams())
                .makerUserId(request.getMakerUserId())
                .makerUserName(request.getMakerUserName())
                .makerRoles(request.getMakerRoles())
                .makerProductCode(request.getMakerProductCode())
                .expiresAt(expiresAt)
                .build();

        approval = approvalRepository.save(approval);
        logAudit(approval, AuditAction.CREATED, request.getMakerUserId(), "Approval request created");

        log.info("Created approval request {} for {} {} by {}",
                approval.getId(), request.getHttpMethod(), request.getRequestPath(),
                request.getMakerUserId());

        return toResponse(approval);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponse> getApprovals(ApprovalStatus status, String makerUserId,
            Pageable pageable) {
        Page<ApprovalRequest> page;
        if (status != null && makerUserId != null) {
            page = approvalRepository.findByStatusAndMakerUserId(status, makerUserId, pageable);
        } else if (status != null) {
            page = approvalRepository.findByStatus(status, pageable);
        } else if (makerUserId != null) {
            page = approvalRepository.findByMakerUserId(makerUserId, pageable);
        } else {
            page = approvalRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getApproval(UUID id) {
        return toResponse(findOrThrow(id));
    }

    /**
     * Approve a pending request and execute it via the gateway.
     */
    @Transactional
    public ApprovalRequestResponse approve(UUID id, ApprovalActionRequest action,
            String checkerUserId, String checkerUserName) {
        ApprovalRequest approval = findOrThrow(id);
        validatePending(approval);
        validateCheckerIsNotMaker(approval, checkerUserId);

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setCheckerUserId(checkerUserId);
        approval.setCheckerUserName(checkerUserName);
        approval.setCheckerComment(action != null ? action.getComment() : null);
        approval.setResolvedAt(OffsetDateTime.now());
        approval = approvalRepository.save(approval);

        logAudit(approval, AuditAction.APPROVED, checkerUserId,
                "Approved" + (action != null && action.getComment() != null
                        ? ": " + action.getComment()
                        : ""));

        log.info("Approval {} approved by {}", id, checkerUserId);

        // Execute the original request via gateway
        try {
            var result = executionService.executeApprovedRequest(approval);
            approval.setStatus(ApprovalStatus.EXECUTED);
            approval.setResponseStatus(result.statusCode());
            approval.setResponseBody(result.body());
            approvalRepository.save(approval);

            logAudit(approval, AuditAction.EXECUTED, checkerUserId,
                    "Executed with status " + result.statusCode());
            log.info("Approval {} executed successfully with status {}", id, result.statusCode());
        } catch (Exception e) {
            approval.setStatus(ApprovalStatus.FAILED);
            approval.setResponseBody("Execution failed: " + e.getMessage());
            approvalRepository.save(approval);

            logAudit(approval, AuditAction.FAILED, checkerUserId,
                    "Execution failed: " + e.getMessage());
            log.error("Approval {} execution failed", id, e);
        }

        return toResponse(approval);
    }

    /**
     * Reject a pending request.
     */
    @Transactional
    public ApprovalRequestResponse reject(UUID id, ApprovalActionRequest action,
            String checkerUserId, String checkerUserName) {
        ApprovalRequest approval = findOrThrow(id);
        validatePending(approval);

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setCheckerUserId(checkerUserId);
        approval.setCheckerUserName(checkerUserName);
        approval.setCheckerComment(action != null ? action.getComment() : null);
        approval.setResolvedAt(OffsetDateTime.now());
        approval = approvalRepository.save(approval);

        logAudit(approval, AuditAction.REJECTED, checkerUserId,
                "Rejected" + (action != null && action.getComment() != null
                        ? ": " + action.getComment()
                        : ""));

        log.info("Approval {} rejected by {}", id, checkerUserId);
        return toResponse(approval);
    }

    // ── Validation ──

    private void validatePending(ApprovalRequest approval) {
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Approval is not pending. Current status: " + approval.getStatus());
        }
    }

    private void validateCheckerIsNotMaker(ApprovalRequest approval, String checkerUserId) {
        if (!approval.getConfig().isSameMakerCanCheck() &&
                approval.getMakerUserId().equals(checkerUserId)) {
            throw new IllegalArgumentException(
                    "Same user cannot be both maker and checker for this operation");
        }
    }

    // ── Helpers ──

    private ApprovalRequest findOrThrow(UUID id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Approval not found: " + id));
    }

    private void logAudit(ApprovalRequest approval, AuditAction action,
            String performedBy, String details) {
        ApprovalAuditLog auditLog = ApprovalAuditLog.builder()
                .approval(approval)
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }

    private ApprovalRequestResponse toResponse(ApprovalRequest approval) {
        MakerCheckerConfig config = approval.getConfig();
        return ApprovalRequestResponse.builder()
                .id(approval.getId())
                .configId(config.getId())
                .status(approval.getStatus())
                .httpMethod(approval.getHttpMethod())
                .requestPath(approval.getRequestPath())
                .requestBody(approval.getRequestBody())
                .requestHeaders(approval.getRequestHeaders())
                .queryParams(approval.getQueryParams())
                .makerUserId(approval.getMakerUserId())
                .makerUserName(approval.getMakerUserName())
                .makerRoles(approval.getMakerRoles())
                .makerProductCode(approval.getMakerProductCode())
                .checkerUserId(approval.getCheckerUserId())
                .checkerUserName(approval.getCheckerUserName())
                .checkerComment(approval.getCheckerComment())
                .responseStatus(approval.getResponseStatus())
                .responseBody(approval.getResponseBody())
                .serviceName(config.getServiceName())
                .endpointPattern(config.getEndpointPattern())
                .configDescription(config.getDescription())
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .expiresAt(approval.getExpiresAt())
                .resolvedAt(approval.getResolvedAt())
                .build();
    }
}

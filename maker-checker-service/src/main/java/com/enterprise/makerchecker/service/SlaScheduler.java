package com.enterprise.makerchecker.service;

import com.enterprise.makerchecker.entity.ApprovalRequest;
import com.enterprise.makerchecker.enums.ApprovalStatus;
import com.enterprise.makerchecker.enums.AuditAction;
import com.enterprise.makerchecker.entity.ApprovalAuditLog;
import com.enterprise.makerchecker.repository.ApprovalAuditLogRepository;
import com.enterprise.makerchecker.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job that expires PENDING approvals that have breached their SLA
 * deadline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private final ApprovalRequestRepository approvalRepository;
    private final ApprovalAuditLogRepository auditLogRepository;

    /**
     * Runs every 5 minutes to check for expired approvals.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    @Transactional
    public void expireBreachedApprovals() {
        List<ApprovalRequest> expired = approvalRepository.findExpiredApprovals(
                ApprovalStatus.PENDING, OffsetDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("Found {} expired approvals to process", expired.size());

        for (ApprovalRequest approval : expired) {
            approval.setStatus(ApprovalStatus.EXPIRED);
            approval.setResolvedAt(OffsetDateTime.now());
            approvalRepository.save(approval);

            ApprovalAuditLog auditLog = ApprovalAuditLog.builder()
                    .approval(approval)
                    .action(AuditAction.EXPIRED)
                    .performedBy("SYSTEM")
                    .details("Expired due to SLA breach. Request: " +
                            approval.getHttpMethod() + " " + approval.getRequestPath())
                    .build();
            auditLogRepository.save(auditLog);

            log.info("Expired approval {} ({} {}) - exceeded SLA deadline",
                    approval.getId(), approval.getHttpMethod(), approval.getRequestPath());
        }
    }
}

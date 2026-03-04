package com.enterprise.makerchecker.repository;

import com.enterprise.makerchecker.entity.ApprovalAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalAuditLogRepository extends JpaRepository<ApprovalAuditLog, UUID> {

    List<ApprovalAuditLog> findByApprovalIdOrderByPerformedAtAsc(UUID approvalId);
}

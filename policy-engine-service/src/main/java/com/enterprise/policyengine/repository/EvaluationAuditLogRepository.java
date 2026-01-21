package com.enterprise.policyengine.repository;

import com.enterprise.policyengine.entity.EvaluationAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationAuditLogRepository extends JpaRepository<EvaluationAuditLog, UUID> {

    Page<EvaluationAuditLog> findBySubjectIdOrderByEvaluatedAtDesc(UUID subjectId, Pageable pageable);

    Page<EvaluationAuditLog> findByPolicyIdOrderByEvaluatedAtDesc(UUID policyId, Pageable pageable);

    List<EvaluationAuditLog> findByEvaluatedAtBetween(Instant start, Instant end);

    long countByDecision(String decision);
}

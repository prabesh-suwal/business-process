package com.enterprise.policyengine.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * EvaluationAuditLog - records every authorization evaluation for auditing.
 */
@Entity
@Table(name = "evaluation_audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "policy_id")
    private UUID policyId;

    @Column(name = "policy_name", length = 100)
    private String policyName;

    @Column(name = "subject_id", nullable = false)
    private String subjectId;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 10)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evaluation_time_ms")
    private Integer evaluationTimeMs;

    @Type(JsonType.class)
    @Column(name = "request_context", columnDefinition = "jsonb")
    private Map<String, Object> requestContext;

    @CreatedDate
    @Column(name = "evaluated_at", updatable = false)
    private Instant evaluatedAt;
}

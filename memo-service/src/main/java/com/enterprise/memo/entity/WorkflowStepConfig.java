package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Stores per-step workflow configuration for a memo topic.
 * This includes assignment rules, form config, SLA, and escalation.
 */
@Entity
@Table(name = "workflow_step_config", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "memo_topic_id", "task_key" })
}, indexes = {
                @Index(name = "idx_step_config_topic", columnList = "memo_topic_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepConfig {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "memo_topic_id", nullable = false)
        private MemoTopic memoTopic;

        @Column(name = "task_key", nullable = false, length = 100)
        private String taskKey; // e.g., "creditReview", "branchManagerApproval"

        @Column(name = "task_name")
        private String taskName; // Human readable: "Credit Review"

        @Column(name = "step_order")
        private Integer stepOrder; // Order in workflow (for display)

        // Assignment configuration
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "assignment_config", columnDefinition = "jsonb")
        private Map<String, Object> assignmentConfig;
        // Structure: {type, role, scope, fallback, conditions, completionRule, quorum}

        // Form configuration
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "form_config", columnDefinition = "jsonb")
        private Map<String, Object> formConfig;
        // Structure: {formCode, editableFields, mandatoryFields, mode}

        // SLA configuration
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "sla_config", columnDefinition = "jsonb")
        private Map<String, Object> slaConfig;
        // Structure: {duration, warningBefore, calendar}

        // Escalation rules (can have multiple levels)
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "escalation_config", columnDefinition = "jsonb")
        private Map<String, Object> escalationConfig;
        // Structure: {escalations: [{level, after, action, roles}]}

        // Step-specific viewer configuration
        // Users, roles, or departments that can view tasks for this specific step
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "viewer_config", columnDefinition = "jsonb")
        private Map<String, Object> viewerConfig;

        // Condition/branching configuration
        // Defines routing conditions for workflow transitions
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "condition_config", columnDefinition = "jsonb")
        private Map<String, Object> conditionConfig;

        // Outcome configuration (action buttons / decision routing)
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "outcome_config", columnDefinition = "jsonb")
        private Map<String, Object> outcomeConfig;

        @Column(nullable = false)
        @Builder.Default
        private Boolean active = true;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;
}

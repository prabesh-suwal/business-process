package com.enterprise.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks every variable change in a process instance for complete audit trail.
 */
@Entity
@Table(name = "variable_audit", indexes = {
        @Index(name = "idx_variable_audit_process", columnList = "process_instance_id"),
        @Index(name = "idx_variable_audit_task", columnList = "task_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariableAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "process_instance_id", nullable = false, length = 64)
    private String processInstanceId;

    @Column(name = "task_id", length = 64)
    private String taskId;

    @Column(name = "variable_name", nullable = false)
    private String variableName;

    @Column(name = "variable_type", length = 100)
    private String variableType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Map<String, Object> oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private Map<String, Object> newValue;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_by_name")
    private String changedByName;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;
}

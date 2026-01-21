package com.enterprise.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TaskConfiguration - per-task settings for workflow tasks.
 * Defines SLA, maker-checker, assignment, notifications, and form mappings.
 */
@Entity
@Table(name = "task_configuration", indexes = {
        @Index(name = "idx_task_config_template", columnList = "process_template_id"),
        @Index(name = "idx_task_config_key", columnList = "task_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_template_id", nullable = false)
    private ProcessTemplate processTemplate;

    // BPMN task ID (e.g., "creditReviewTask")
    @Column(name = "task_key", nullable = false)
    private String taskKey;

    // Human readable name
    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Task order in workflow (for display)
    @Column(name = "task_order")
    @Builder.Default
    private Integer taskOrder = 0;

    // === FORM MAPPING ===

    @Column(name = "form_id")
    private UUID formId;

    @Column(name = "form_version")
    private Integer formVersion;

    // === MAKER-CHECKER ===

    @Column(name = "requires_maker_checker")
    @Builder.Default
    private Boolean requiresMakerChecker = false;

    // Checker role codes (JSON array)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checker_roles", columnDefinition = "jsonb")
    private List<String> checkerRoles;

    // === SLA ===

    @Column(name = "sla_hours")
    private Integer slaHours;

    @Column(name = "warning_hours")
    private Integer warningHours; // Hours before SLA to send warning

    @Column(name = "escalation_role")
    private String escalationRole; // Role to escalate to on SLA breach

    // === RETURN / REWORK ===

    // Task keys this task can return to
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "can_return_to", columnDefinition = "jsonb")
    private List<String> canReturnTo;

    // === NOTIFICATIONS ===

    // Template code for task assignment notification
    @Column(name = "assignment_notification_code")
    private String assignmentNotificationCode;

    // Template code for task completion notification
    @Column(name = "completion_notification_code")
    private String completionNotificationCode;

    // Template code for SLA warning
    @Column(name = "sla_warning_notification_code")
    private String slaWarningNotificationCode;

    // Template code for SLA breach
    @Column(name = "sla_breach_notification_code")
    private String slaBreachNotificationCode;

    // === ASSIGNMENT OVERRIDE ===

    // Override default assignment (JSON structure)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assignment_config", columnDefinition = "jsonb")
    private Map<String, Object> assignmentConfig;

    // === ADDITIONAL CONFIG ===

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

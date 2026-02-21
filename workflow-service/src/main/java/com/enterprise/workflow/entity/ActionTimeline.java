package com.enterprise.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Complete timeline of all actions in a process instance.
 * Records who did what, when, with full context.
 */
@Entity
@Table(name = "action_timeline", indexes = {
        @Index(name = "idx_action_timeline_process", columnList = "process_instance_id"),
        @Index(name = "idx_action_timeline_actor", columnList = "actor_id"),
        @Index(name = "idx_action_timeline_type", columnList = "action_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "process_instance_id", nullable = false, length = 64)
    private String processInstanceId;

    @Column(name = "action_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(name = "task_id", length = 64)
    private String taskId;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name")
    private String actorName;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "actor_roles", columnDefinition = "text[]")
    private List<String> actorRoles;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        PROCESS_STARTED,
        PROCESS_COMPLETED,
        PROCESS_CANCELLED,
        TASK_CREATED,
        TASK_ASSIGNED,
        TASK_CLAIMED,
        TASK_COMPLETED,
        TASK_DELEGATED,
        TASK_CANCELLED, // For parallel branch cancellation (first approval wins)
        VARIABLE_UPDATED,
        FORM_SUBMITTED,
        DOCUMENT_UPLOADED,
        COMMENT_ADDED,
        INTEGRATION_CALLED,
        NOTIFICATION_SENT,
        TASK_SENT_BACK,
        TASK_REJECTED,
        MEMO_SUBMITTED // When memo is submitted to begin the workflow
    }
}

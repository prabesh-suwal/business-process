package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks workflow tasks in memo context.
 * This is the memo-service's view of a Flowable task.
 */
@Entity
@Table(name = "memo_task", indexes = {
        @Index(name = "idx_memo_task_memo", columnList = "memo_id"),
        @Index(name = "idx_memo_task_workflow", columnList = "workflow_task_id"),
        @Index(name = "idx_memo_task_assignee", columnList = "assigned_to"),
        @Index(name = "idx_memo_task_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    // Flowable task reference
    @Column(name = "workflow_task_id", nullable = false)
    private String workflowTaskId;

    @Column(name = "task_definition_key")
    private String taskDefinitionKey; // e.g., "rm_review", "bm_approval"

    @Column(name = "task_name")
    private String taskName; // Human readable: "RM Review"

    @Column(name = "stage")
    private String stage; // Current workflow stage

    // Assignment
    @Column(name = "assigned_to")
    private String assignedTo; // User ID

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(name = "candidate_groups")
    private String candidateGroups; // Comma-separated

    @Column(name = "candidate_users")
    private String candidateUsers; // Comma-separated

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    // Action taken
    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken")
    private TaskAction actionTaken;

    @Column(columnDefinition = "TEXT")
    private String comments;

    // SLA
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 50;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TaskStatus {
        PENDING, // Waiting for action
        CLAIMED, // User claimed the task
        COMPLETED, // Task completed
        CANCELLED // Task cancelled
    }

    public enum TaskAction {
        APPROVE,
        REJECT,
        SEND_BACK,
        FORWARD,
        CLARIFY
    }
}

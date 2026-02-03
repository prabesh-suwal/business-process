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

@Entity
@Table(name = "memo", indexes = {
        @Index(name = "idx_memo_number", columnList = "memo_number"),
        @Index(name = "idx_memo_status", columnList = "status"),
        @Index(name = "idx_memo_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private MemoTopic topic;

    @Column(name = "memo_number", nullable = false, unique = true)
    private String memoNumber;

    @Column(nullable = false)
    private String subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MemoCategory category;

    @Column(nullable = false)
    private String priority; // NORMAL, HIGH, URGENT

    // Rich Text Content (HTML/JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> content;

    // Structured Data from Form (e.g. Amount, Vendor)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> formData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoStatus status;

    // Workflow State
    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "current_stage")
    private String currentStage;

    @Column(name = "current_assignee")
    private String currentAssignee; // User ID or Role

    // Custom workflow overrides (when user customizes workflow for this memo)
    // Contains: {customWorkflow: true, steps: [...]}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "workflow_overrides", columnDefinition = "jsonb")
    private Map<String, Object> workflowOverrides;

    // Track which workflow version this memo uses
    // Memos continue using their original version even if topic is updated
    @Column(name = "workflow_version")
    @Builder.Default
    private Integer workflowVersion = 1;

    // Audit
    @Column(name = "created_by")
    private UUID createdBy; // User UUID

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

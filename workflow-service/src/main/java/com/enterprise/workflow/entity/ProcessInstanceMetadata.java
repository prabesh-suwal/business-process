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
 * Extended metadata for process instances beyond what Flowable stores.
 */
@Entity
@Table(name = "process_instance_metadata", indexes = {
        @Index(name = "idx_process_metadata_flowable", columnList = "flowable_process_instance_id"),
        @Index(name = "idx_process_metadata_product", columnList = "product_id"),
        @Index(name = "idx_process_metadata_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessInstanceMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flowable_process_instance_id", nullable = false, unique = true, length = 64)
    private String flowableProcessInstanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_template_id")
    private ProcessTemplate processTemplate;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "business_key")
    private String businessKey;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "started_by")
    private UUID startedBy;

    @Column(name = "started_by_name")
    private String startedByName;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProcessInstanceStatus status = ProcessInstanceStatus.RUNNING;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public enum ProcessInstanceStatus {
        RUNNING,
        COMPLETED,
        CANCELLED,
        SUSPENDED
    }
}

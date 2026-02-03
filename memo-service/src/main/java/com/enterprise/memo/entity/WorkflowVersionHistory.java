package com.enterprise.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Stores workflow version snapshots.
 * When a new version is created, the current deployed state is snapshotted here
 * so running memo instances continue to work with their original workflow.
 */
@Entity
@Table(name = "workflow_version_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "topic_id", "version" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowVersionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "workflow_xml", columnDefinition = "TEXT", nullable = false)
    private String workflowXml;

    // Flowable process definition ID for this version
    @Column(name = "workflow_template_id")
    private String workflowTemplateId;

    // Snapshot of step configurations at time of deployment
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_configs_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> stepConfigsSnapshot;

    // Snapshot of gateway decision rules at time of deployment
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_rules_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> gatewayRulesSnapshot;

    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

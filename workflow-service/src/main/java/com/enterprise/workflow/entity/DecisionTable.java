package com.enterprise.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DecisionTable links our business metadata to Flowable DMN decision
 * definitions.
 * Admins create decision tables in the UI, which are then deployed to the
 * Flowable DMN engine.
 * Supports versioning with effective dates (mirrors ProcessTemplate pattern).
 */
@Entity
@Table(name = "decision_table", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "product_id", "key", "version" })
}, indexes = {
        @Index(name = "idx_decision_product", columnList = "product_id"),
        @Index(name = "idx_decision_status", columnList = "status"),
        @Index(name = "idx_decision_key", columnList = "key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_code", length = 20)
    private String productCode;

    @Column(nullable = false)
    private String name;

    /**
     * Unique key for referencing this decision table from BPMN Business Rule Tasks.
     * Convention: lowercase-hyphenated, e.g. "memo-routing-rules"
     */
    @Column(nullable = false)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "dmn_xml", columnDefinition = "TEXT")
    private String dmnXml;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DecisionTableStatus status = DecisionTableStatus.DRAFT;

    // === FLOWABLE REFERENCES ===

    @Column(name = "flowable_deployment_id")
    private String flowableDeploymentId;

    @Column(name = "flowable_decision_key")
    private String flowableDecisionKey;

    // === VERSIONING ===

    @Column(name = "previous_version_id")
    private UUID previousVersionId;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    // === AUDIT ===

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DecisionTableStatus {
        DRAFT,
        ACTIVE,
        DEPRECATED
    }

    /**
     * Check if this decision table is currently effective.
     */
    public boolean isEffective() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = effectiveFrom == null || !now.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || now.isBefore(effectiveTo);
        return afterStart && beforeEnd && status == DecisionTableStatus.ACTIVE;
    }
}

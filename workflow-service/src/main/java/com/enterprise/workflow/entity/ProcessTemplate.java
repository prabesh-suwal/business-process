package com.enterprise.workflow.entity;

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
 * ProcessTemplate links our business metadata to Flowable process definitions.
 * Admin creates templates in the UI, which are then deployed to Flowable.
 * Supports versioning with effective dates.
 */
@Entity
@Table(name = "process_template", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "product_id", "name", "version" })
}, indexes = {
        @Index(name = "idx_template_product", columnList = "product_id"),
        @Index(name = "idx_template_status", columnList = "status"),
        @Index(name = "idx_template_effective", columnList = "effective_from, effective_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // Product code for easy identification (MMS, LMS, etc.)
    @Column(name = "product_code", length = 20)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "flowable_process_def_key")
    private String flowableProcessDefKey;

    @Column(name = "flowable_deployment_id")
    private String flowableDeploymentId;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProcessTemplateStatus status = ProcessTemplateStatus.DRAFT;

    @Column(name = "bpmn_xml", columnDefinition = "TEXT")
    private String bpmnXml;

    // === VERSIONING ===

    // When this version becomes active
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    // When this version expires (null = forever)
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    // Previous version of this template (for version chain)
    @Column(name = "previous_version_id")
    private UUID previousVersionId;

    // === FORM LINKS ===

    // Start form for initiating this process
    @Column(name = "start_form_id")
    private UUID startFormId;

    // Version of start form (for immutability)
    @Column(name = "start_form_version")
    private Integer startFormVersion;

    // === CONFIGURATION ===

    // Default SLA hours for the entire process
    @Column(name = "default_sla_hours")
    private Integer defaultSlaHours;

    // Additional process-level config
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

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

    public enum ProcessTemplateStatus {
        DRAFT,
        ACTIVE,
        DEPRECATED
    }

    /**
     * Check if this template is currently effective.
     */
    public boolean isEffective() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = effectiveFrom == null || !now.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || now.isBefore(effectiveTo);
        return afterStart && beforeEnd && status == ProcessTemplateStatus.ACTIVE;
    }
}

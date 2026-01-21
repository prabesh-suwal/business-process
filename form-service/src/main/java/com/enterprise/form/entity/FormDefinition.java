package com.enterprise.form.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Form definition with JSON Schema based structure.
 * Supports versioning - once ACTIVE, form becomes immutable.
 */
@Entity
@Table(name = "form_definition", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "product_id", "name", "version" })
}, indexes = {
        @Index(name = "idx_form_product", columnList = "product_id"),
        @Index(name = "idx_form_status", columnList = "status"),
        @Index(name = "idx_form_type", columnList = "form_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    // Form type for categorization
    @Column(name = "form_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FormType formType = FormType.GENERAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> schema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_schema", columnDefinition = "jsonb")
    private Map<String, Object> uiSchema;

    // Layout configuration for multi-step/sectioned forms
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_config", columnDefinition = "jsonb")
    private Map<String, Object> layoutConfig;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FormStatus status = FormStatus.DRAFT;

    // === VERSIONING ===

    // Link to previous version
    @Column(name = "previous_version_id")
    private UUID previousVersionId;

    // When this version was published/activated
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // Who published this version
    @Column(name = "published_by")
    private UUID publishedBy;

    // === VALIDATION ===

    // Custom validation rules (JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private Map<String, Object> validationRules;

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

    @OneToMany(mappedBy = "formDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FieldDefinition> fields = new ArrayList<>();

    public enum FormStatus {
        DRAFT, // Can be edited
        ACTIVE, // Published, immutable
        DEPRECATED // No longer in use
    }

    public enum FormType {
        GENERAL, // Generic form
        START_FORM, // Process start form
        TASK_FORM, // Task completion form
        APPROVAL_FORM, // Approval decision form
        DOCUMENT_FORM, // Document collection form
        CUSTOMER_FORM // Customer-facing form
    }

    /**
     * Check if this form can be edited.
     */
    public boolean isEditable() {
        return status == FormStatus.DRAFT;
    }
}

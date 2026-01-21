package com.enterprise.form.entity;

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
 * FormDraft - stores partial/incomplete form submissions.
 * Allows users to save progress and continue later.
 */
@Entity
@Table(name = "form_draft", indexes = {
        @Index(name = "idx_draft_form", columnList = "form_definition_id"),
        @Index(name = "idx_draft_user", columnList = "user_id"),
        @Index(name = "idx_draft_entity", columnList = "linked_entity_type, linked_entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    private FormDefinition formDefinition;

    @Column(name = "form_version")
    private Integer formVersion;

    // User who owns this draft
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name")
    private String userName;

    // Partial form data (JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> formData;

    // Track which fields have been filled
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completed_fields", columnDefinition = "jsonb")
    private Map<String, Boolean> completedFields;

    // Current step for multi-step forms
    @Column(name = "current_step")
    @Builder.Default
    private Integer currentStep = 0;

    // Total steps (for progress tracking)
    @Column(name = "total_steps")
    private Integer totalSteps;

    // Link to business entity (e.g., loan application being created)
    @Column(name = "linked_entity_type")
    private String linkedEntityType;

    @Column(name = "linked_entity_id")
    private UUID linkedEntityId;

    // Optional: session/context info
    @Column(name = "context")
    private String context; // e.g., "loan-application-create"

    // Auto-save or manual save
    @Column(name = "is_auto_save")
    @Builder.Default
    private Boolean isAutoSave = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Drafts expire after certain period
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Calculate completion percentage.
     */
    public int getCompletionPercentage() {
        if (completedFields == null || completedFields.isEmpty()) {
            return 0;
        }
        long completed = completedFields.values().stream().filter(v -> v).count();
        return (int) ((completed * 100) / completedFields.size());
    }

    /**
     * Check if draft has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}

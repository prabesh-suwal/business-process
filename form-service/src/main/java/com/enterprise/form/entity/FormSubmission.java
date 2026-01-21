package com.enterprise.form.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Form submission record with submitted data.
 */
@Entity
@Table(name = "form_submission", indexes = {
        @Index(name = "idx_submission_form", columnList = "form_definition_id"),
        @Index(name = "idx_submission_process", columnList = "process_instance_id"),
        @Index(name = "idx_submission_task", columnList = "task_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    private FormDefinition formDefinition;

    @Column(name = "process_instance_id", length = 64)
    private String processInstanceId;

    @Column(name = "task_id", length = 64)
    private String taskId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "submitted_by_name")
    private String submittedByName;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "validation_status", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ValidationStatus validationStatus = ValidationStatus.VALID;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private List<Map<String, Object>> validationErrors;

    @OneToMany(mappedBy = "formSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FileUpload> files = new ArrayList<>();

    public enum ValidationStatus {
        VALID,
        INVALID,
        PENDING
    }
}

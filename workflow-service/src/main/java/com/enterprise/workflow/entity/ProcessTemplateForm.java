package com.enterprise.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps form definitions to specific tasks in a process template.
 */
@Entity
@Table(name = "process_template_form", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "process_template_id", "task_key" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessTemplateForm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_template_id", nullable = false)
    private ProcessTemplate processTemplate;

    @Column(name = "task_key", nullable = false)
    private String taskKey;

    @Column(name = "form_definition_id", nullable = false)
    private UUID formDefinitionId;

    @Column(name = "form_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FormType formType = FormType.TASK_FORM;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum FormType {
        START_FORM,
        TASK_FORM
    }
}

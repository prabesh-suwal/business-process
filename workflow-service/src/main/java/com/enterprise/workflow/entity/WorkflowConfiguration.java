package com.enterprise.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Table(name = "workflow_configuration")
public class WorkflowConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_template_id")
    private ProcessTemplate processTemplate;

    @Column(name = "start_form_id")
    private UUID startFormId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "task_form_mappings", columnDefinition = "jsonb")
    private Map<String, UUID> taskFormMappings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assignment_rules", columnDefinition = "jsonb")
    private Map<String, Object> assignmentRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

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
@Table(name = "memo_topic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MemoCategory category;

    @Column(nullable = false, unique = true)
    private String code; // CAPEX, NEW_HIRE

    @Column(nullable = false)
    private String name; // Capital Expense Request

    @Column(columnDefinition = "TEXT")
    private String description;

    // Link to Workflow Service - Flowable process definition ID (not a UUID)
    @Column(name = "workflow_template_id")
    private String workflowTemplateId;

    // Link to Form Service (for structured data)
    @Column(name = "form_definition_id")
    private UUID formDefinitionId;

    // Rich Text Template (HTML/JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_template", columnDefinition = "jsonb")
    private Map<String, Object> contentTemplate;

    // Numbering Pattern e.g. "CPX-%FY%-%SEQ%"
    @Column(name = "numbering_pattern")
    private String numberingPattern;

    // BPMN Workflow XML for this topic's approval process
    @Column(name = "workflow_xml", columnDefinition = "TEXT")
    private String workflowXml;

    // JSON Schema for dynamic form fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_schema", columnDefinition = "jsonb")
    private Map<String, Object> formSchema;

    // Assignment rules per task (keyed by task definition key)
    // e.g., {"rm_review": {"strategy": "ROLE", "role": "RELATIONSHIP_MANAGER",
    // "scope": "ORIGINATING_BRANCH"}}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assignment_rules", columnDefinition = "jsonb")
    private Map<String, Object> assignmentRules;

    // SLA rules per task
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sla_rules", columnDefinition = "jsonb")
    private Map<String, Object> slaRules;

    // Escalation rules
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "escalation_rules", columnDefinition = "jsonb")
    private Map<String, Object> escalationRules;

    // Memo-wide viewer configuration
    // Users, roles, or departments that can view this entire memo and all its tasks
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "viewer_config", columnDefinition = "jsonb")
    private Map<String, Object> viewerConfig;

    // Override permissions - what users can customize when creating memos
    // {allowOverrideAssignments: true, allowOverrideSLA: false, ...}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "override_permissions", columnDefinition = "jsonb")
    private Map<String, Object> overridePermissions;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Workflow version number (v1, v2, v3...)
    @Column(name = "workflow_version")
    @Builder.Default
    private Integer workflowVersion = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if workflow is deployed (locked for editing).
     * A workflow is deployed when workflowTemplateId is set.
     */
    public boolean isWorkflowDeployed() {
        return workflowTemplateId != null && !workflowTemplateId.isBlank();
    }
}

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

    // Link to Workflow Service
    @Column(name = "workflow_template_id")
    private UUID workflowTemplateId;

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

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

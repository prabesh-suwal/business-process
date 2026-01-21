package com.enterprise.form.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Field definition within a form.
 * Represents individual form fields with their configuration.
 */
@Entity
@Table(name = "field_definition", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "form_definition_id", "field_key" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    private FormDefinition formDefinition;

    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(name = "field_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FieldType fieldType;

    @Column
    private String label;

    @Column
    private String placeholder;

    @Column(name = "help_text", columnDefinition = "TEXT")
    private String helpText;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private Map<String, Object> validationRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visibility_rules", columnDefinition = "jsonb")
    private Map<String, Object> visibilityRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private Map<String, Object> options;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_value", columnDefinition = "jsonb")
    private Object defaultValue;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "group_name")
    private String groupName;

    // Layout properties for enterprise form designer
    @Column(name = "element_type", length = 20)
    @Builder.Default
    private String elementType = "field"; // 'field' or 'layout'

    @Column(name = "width", length = 20)
    @Builder.Default
    private String width = "full"; // 'full', 'half', 'third', 'quarter'

    @Column(name = "custom_width")
    private Integer customWidth; // Custom width in pixels

    @Column(name = "custom_height")
    private Integer customHeight; // Custom height in pixels

    @Column(name = "label_position", length = 20)
    @Builder.Default
    private String labelPosition = "top"; // 'top' or 'left'

    @Column(name = "section_id")
    private String sectionId; // Reference to parent section

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum FieldType {
        TEXT,
        TEXTAREA,
        NUMBER,
        DATE,
        DATETIME,
        DROPDOWN,
        DROPDOWN_DYNAMIC,
        MULTI_SELECT,
        CHECKBOX,
        RADIO,
        FILE,
        SIGNATURE,
        TABLE,
        CALCULATED,
        REFERENCE,
        HIDDEN,
        // Layout elements
        SECTION,
        DIVIDER,
        HEADING
    }
}

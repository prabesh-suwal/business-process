package com.enterprise.form.dto;

import com.enterprise.form.entity.FieldDefinition.FieldType;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldDefinitionDTO {

    private UUID id;
    private String fieldKey;
    private FieldType fieldType;
    private String label;
    private String placeholder;
    private String helpText;
    private Boolean required;
    private Map<String, Object> validationRules;
    private Map<String, Object> visibilityRules;
    private Map<String, Object> options;
    private Object defaultValue;
    private Integer displayOrder;
    private String groupName;

    // Layout properties for enterprise form designer
    private String elementType; // 'field' or 'layout'
    private String width; // 'full', 'half', 'third', 'quarter'
    private Integer customWidth; // Custom width in pixels
    private Integer customHeight; // Custom height in pixels
    private String labelPosition; // 'top' or 'left'
    private String sectionId; // Reference to parent section
}

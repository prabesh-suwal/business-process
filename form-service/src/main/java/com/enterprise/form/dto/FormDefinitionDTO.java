package com.enterprise.form.dto;

import com.enterprise.form.entity.FormDefinition.FormStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormDefinitionDTO {

    private UUID id;
    private UUID productId;
    private String name;
    private String description;
    private Integer version;
    private Map<String, Object> schema;
    private Map<String, Object> uiSchema;
    private FormStatus status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FieldDefinitionDTO> fields;
}

package com.enterprise.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFormRequest {

    private UUID productId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Schema is required")
    private Map<String, Object> schema;

    private Map<String, Object> uiSchema;

    private List<FieldDefinitionDTO> fields;
}

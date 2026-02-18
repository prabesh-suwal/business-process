package com.enterprise.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateDecisionTableRequest {

    @NotBlank(message = "Name is required")
    private String name;

    /**
     * Unique key for referencing from BPMN. Must be lowercase-hyphenated.
     * Example: "memo-routing-rules"
     */
    @NotBlank(message = "Key is required")
    @Pattern(regexp = "^[a-z][a-z0-9-]*$", message = "Key must be lowercase-hyphenated (e.g. memo-routing-rules)")
    private String key;

    private String description;

    /**
     * Optional initial DMN XML. If null, a default template will be generated.
     */
    private String dmnXml;

    private UUID productId;

    private String productCode;
}

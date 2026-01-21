package com.enterprise.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class CreateWorkflowConfigRequest {

    @NotBlank(message = "Product code is required")
    @Size(max = 50)
    private String productCode;

    @NotBlank(message = "Configuration code is required")
    @Size(max = 100)
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private UUID processTemplateId;

    private UUID startFormId;

    private Map<String, UUID> taskFormMappings;

    private Map<String, Object> assignmentRules;

    private Map<String, Object> config;
}

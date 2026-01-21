package com.enterprise.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class WorkflowConfigurationDTO {
    private UUID id;
    private String productCode;
    private String code;
    private String name;
    private String description;
    private UUID processTemplateId;
    private String processTemplateName;
    private UUID startFormId;
    private Map<String, UUID> taskFormMappings;
    private Map<String, Object> assignmentRules;
    private Map<String, Object> config;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.enterprise.form.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDraftRequest {
    private UUID formDefinitionId;
    private Map<String, Object> formData;
    private Map<String, Boolean> completedFields;
    private Integer currentStep;
    private Integer totalSteps;
    private String linkedEntityType;
    private UUID linkedEntityId;
    private String context;
    private Boolean isAutoSave;
}

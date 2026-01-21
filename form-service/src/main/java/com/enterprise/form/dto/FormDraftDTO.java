package com.enterprise.form.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormDraftDTO {
    private UUID id;
    private UUID formDefinitionId;
    private String formName;
    private Integer formVersion;
    private UUID userId;
    private String userName;
    private Map<String, Object> formData;
    private Map<String, Boolean> completedFields;
    private Integer currentStep;
    private Integer totalSteps;
    private Integer completionPercentage;
    private String linkedEntityType;
    private UUID linkedEntityId;
    private String context;
    private Boolean isAutoSave;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
}

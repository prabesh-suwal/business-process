package com.enterprise.workflow.dto;

import com.enterprise.workflow.entity.ProcessTemplateForm.FormType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for ProcessTemplateForm - maps forms to workflow tasks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessTemplateFormDTO {

    private UUID id;
    private UUID processTemplateId;
    private String taskKey;
    private UUID formDefinitionId;
    private FormType formType;
    private LocalDateTime createdAt;

    // Enriched form metadata (populated from form-service when needed)
    private String formName;
    private String formDescription;
}

package com.enterprise.workflow.dto;

import com.enterprise.workflow.entity.ProcessTemplateForm.FormType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request to attach a form to a workflow task.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachFormRequest {

    @NotBlank(message = "Task key is required")
    private String taskKey;

    @NotNull(message = "Form definition ID is required")
    private UUID formDefinitionId;

    @Builder.Default
    private FormType formType = FormType.TASK_FORM;
}

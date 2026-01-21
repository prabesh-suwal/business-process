package com.enterprise.form.dto;

import com.enterprise.form.entity.FormSubmission.ValidationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSubmissionDTO {

    private UUID id;
    private UUID formDefinitionId;
    private String formName;
    private String processInstanceId;
    private String taskId;
    private Map<String, Object> data;
    private UUID submittedBy;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private ValidationStatus validationStatus;
    private List<Map<String, Object>> validationErrors;
    private List<FileUploadDTO> files;
}

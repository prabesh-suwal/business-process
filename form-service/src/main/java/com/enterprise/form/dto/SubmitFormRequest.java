package com.enterprise.form.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitFormRequest {

    @NotNull(message = "Form Definition ID is required")
    private UUID formDefinitionId;

    private String processInstanceId;

    private String taskId;

    @NotNull(message = "Form data is required")
    private Map<String, Object> data;
}

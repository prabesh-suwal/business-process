package com.enterprise.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartProcessRequest {

    @NotNull(message = "Process Template ID is required")
    private UUID processTemplateId;

    private String businessKey;

    private String title;

    private Map<String, Object> variables;
}

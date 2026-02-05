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

    @NotBlank(message = "Process Template ID is required")
    private String processTemplateId; // Flowable process definition ID or ProcessTemplate UUID

    private String businessKey;

    private String title;

    private Map<String, Object> variables;

    /**
     * If true, processTemplateId is a Flowable process definition ID and
     * startProcessInstanceById will be used. If false (default), the service
     * will detect based on format (UUID vs colon-format vs key).
     */
    private boolean useProcessDefinitionId;
}

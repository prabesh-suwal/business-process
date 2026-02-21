package com.enterprise.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteTaskRequest {

    private Map<String, Object> variables;

    private String comment;

    private boolean approved;

    /**
     * The action label from the configured outcome options
     * (e.g., "Approve", "Reject", "Send Back").
     * When provided, the backend validates this against the TaskConfiguration's
     * outcomeConfig.options[].label. Falls back to approved flag if null.
     */
    private String action;
}

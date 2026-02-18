package com.enterprise.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class EvaluateDecisionRequest {

    @NotBlank(message = "Decision key is required")
    private String decisionKey;

    /**
     * Input variables for DMN evaluation.
     * Keys must match the input column IDs in the decision table.
     */
    private Map<String, Object> variables;
}

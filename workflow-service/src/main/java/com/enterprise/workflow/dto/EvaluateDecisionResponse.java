package com.enterprise.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class EvaluateDecisionResponse {

    /**
     * The decision table key that was evaluated.
     */
    private String decisionKey;

    /**
     * List of result rows. Each row is a map of output variable name → value.
     * For FIRST/UNIQUE hit policy, this will contain exactly one result.
     * For COLLECT/RULE_ORDER, this may contain multiple results.
     */
    private List<Map<String, Object>> results;

    /**
     * The hit policy used for evaluation.
     */
    private String hitPolicy;
}

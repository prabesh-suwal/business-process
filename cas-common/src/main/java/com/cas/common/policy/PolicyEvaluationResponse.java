package com.cas.common.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO from policy evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationResponse {

    /** Whether the request is allowed */
    private boolean allowed;

    /** Effect applied (ALLOW, DENY) */
    private String effect;

    /** Name of the matched policy */
    private String matchedPolicy;

    /** ID of the matched policy */
    private UUID matchedPolicyId;

    /** Human-readable reason for the decision */
    private String reason;

    /** Evaluation duration in milliseconds */
    private long evaluationTimeMs;

    /** Details of rule group evaluations */
    private Map<String, RuleGroupResult> details;

    /** Any obligations to be fulfilled */
    private Map<String, Object> obligations;

    /**
     * Result for a single rule group evaluation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleGroupResult {
        private boolean passed;
        private String message;
    }

    // Factory methods
    public static PolicyEvaluationResponse allowed(String policyName, UUID policyId,
            long evaluationTimeMs, Map<String, RuleGroupResult> details) {
        return PolicyEvaluationResponse.builder()
                .allowed(true)
                .effect("ALLOW")
                .matchedPolicy(policyName)
                .matchedPolicyId(policyId)
                .reason("Access granted")
                .evaluationTimeMs(evaluationTimeMs)
                .details(details)
                .build();
    }

    public static PolicyEvaluationResponse denied(String reason, long evaluationTimeMs,
            Map<String, RuleGroupResult> details) {
        return PolicyEvaluationResponse.builder()
                .allowed(false)
                .effect("DENY")
                .reason(reason)
                .evaluationTimeMs(evaluationTimeMs)
                .details(details)
                .build();
    }

    public static PolicyEvaluationResponse denied(String reason) {
        return PolicyEvaluationResponse.builder()
                .allowed(false)
                .effect("DENY")
                .reason(reason)
                .build();
    }
}

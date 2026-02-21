package com.enterprise.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Defines a predefined action type that admins can configure for task outcomes.
 * Each action type represents a system capability with pre-wired behavior.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionTypeDefinition {

    /** System identifier, e.g. APPROVE, REJECT, SEND_BACK */
    private String actionType;

    /** Default button label shown to users */
    private String defaultLabel;

    /** Human-readable description for admin UI */
    private String description;

    /** Default visual style: success, danger, warning, info, default */
    private String defaultStyle;

    /** Default process variables set when this action is taken */
    private Map<String, String> defaultSets;

    /** Whether a comment is required for this action */
    private boolean requiresComment;

    /** Whether this action requires selecting a target step (e.g., send-back) */
    private boolean requiresTargetStep;
}

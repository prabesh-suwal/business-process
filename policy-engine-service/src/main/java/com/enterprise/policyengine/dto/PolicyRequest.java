package com.enterprise.policyengine.dto;

import com.enterprise.policyengine.entity.Operator;
import com.enterprise.policyengine.entity.PolicyEffect;
import com.enterprise.policyengine.entity.ValueType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Request DTO for creating/updating a policy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRequest {

    @NotBlank(message = "Policy name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    private String description;

    @NotBlank(message = "Resource type is required")
    @Size(max = 50)
    private String resourceType;

    @NotBlank(message = "Action is required")
    @Size(max = 50)
    private String action;

    @NotNull(message = "Effect is required")
    private PolicyEffect effect;

    private Integer priority;

    private Set<String> products; // List of products this policy applies to

    @Valid
    private List<RuleRequest> rules;

    @Valid
    private List<RuleGroupRequest> ruleGroups;

    /**
     * Rule request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleRequest {
        private String ruleGroup;

        @NotBlank(message = "Attribute is required")
        private String attribute;

        @NotNull(message = "Operator is required")
        private Operator operator;

        @NotNull(message = "Value type is required")
        private ValueType valueType;

        // Value can be empty for operators like IS_NULL, IS_TRUE
        private String value;

        private String description;
        private Integer sortOrder;

        // Temporal Conditions
        private com.enterprise.policyengine.entity.TemporalCondition temporalCondition;
        private java.time.LocalTime timeFrom;
        private java.time.LocalTime timeTo;
        private java.time.LocalDate validFrom;
        private java.time.LocalDate validUntil;
        private String timezone;
    }

    /**
     * Rule group request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleGroupRequest {
        @NotBlank(message = "Group name is required")
        private String name;

        private String logicOperator;
        private Integer sortOrder;
    }
}

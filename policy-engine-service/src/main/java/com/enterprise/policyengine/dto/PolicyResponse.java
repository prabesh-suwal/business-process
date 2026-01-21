package com.enterprise.policyengine.dto;

import com.enterprise.policyengine.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for policy queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    private UUID id;
    private String name;
    private String description;
    private String resourceType;
    private String action;
    private PolicyEffect effect;
    private Integer priority;
    private boolean isActive;
    private Integer version;
    private List<RuleResponse> rules;
    private List<RuleGroupResponse> ruleGroups;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleResponse {
        private UUID id;
        private String ruleGroup;
        private String attribute;
        private Operator operator;
        private ValueType valueType;
        private String value;
        private String description;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleGroupResponse {
        private UUID id;
        private String name;
        private LogicOperator logicOperator;
        private Integer sortOrder;
    }

    /**
     * Convert entity to response DTO
     */
    public static PolicyResponse fromEntity(Policy policy) {
        return PolicyResponse.builder()
                .id(policy.getId())
                .name(policy.getName())
                .description(policy.getDescription())
                .resourceType(policy.getResourceType())
                .action(policy.getAction())
                .effect(policy.getEffect())
                .priority(policy.getPriority())
                .isActive(policy.isActive())
                .version(policy.getVersion())
                .createdBy(policy.getCreatedBy())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .rules(policy.getRules().stream()
                        .map(PolicyResponse::ruleToResponse)
                        .collect(Collectors.toList()))
                .ruleGroups(policy.getRuleGroups().stream()
                        .map(PolicyResponse::ruleGroupToResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private static RuleResponse ruleToResponse(PolicyRule rule) {
        return RuleResponse.builder()
                .id(rule.getId())
                .ruleGroup(rule.getRuleGroup())
                .attribute(rule.getAttribute())
                .operator(rule.getOperator())
                .valueType(rule.getValueType())
                .value(rule.getValue())
                .description(rule.getDescription())
                .sortOrder(rule.getSortOrder())
                .build();
    }

    private static RuleGroupResponse ruleGroupToResponse(PolicyRuleGroup group) {
        return RuleGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .logicOperator(group.getLogicOperator())
                .sortOrder(group.getSortOrder())
                .build();
    }
}

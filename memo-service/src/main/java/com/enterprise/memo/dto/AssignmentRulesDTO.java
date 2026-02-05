package com.enterprise.memo.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * DTO for multi-rule assignment configuration.
 * 
 * Rules are combined with OR logic - any matching rule allows the user to see
 * the task.
 * Within each rule, criteria are combined with AND logic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRulesDTO {

    /**
     * List of assignment rules. Rules are combined with OR logic.
     */
    private List<AssignmentRule> rules;

    /**
     * Fallback role ID if no rules match.
     */
    private UUID fallbackRoleId;

    /**
     * Completion mode: ANY (pool-based, any one can complete), ALL (all must
     * complete)
     */
    @Builder.Default
    private String completionMode = "ANY";

    /**
     * Single assignment rule with multiple criteria (AND logic within rule).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentRule {
        private UUID id;
        private String name;
        private AssignmentCriteria criteria;
    }

    /**
     * Criteria for matching users. All non-empty lists are combined with AND logic.
     * For example: regionIds=[R1] AND roleIds=[ROLE_A, ROLE_B]
     * means users in region R1 with either ROLE_A or ROLE_B.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentCriteria {
        /**
         * Region/Province IDs (geo hierarchy level 1)
         */
        private List<UUID> regionIds;

        /**
         * District IDs (geo hierarchy level 2)
         */
        private List<UUID> districtIds;

        /**
         * State/Municipality IDs (geo hierarchy level 3)
         */
        private List<UUID> stateIds;

        /**
         * Branch IDs
         */
        private List<UUID> branchIds;

        /**
         * Department IDs
         */
        private List<UUID> departmentIds;

        /**
         * Group/Committee IDs
         */
        private List<UUID> groupIds;

        /**
         * Role IDs
         */
        private List<UUID> roleIds;

        /**
         * Specific User IDs
         */
        private List<UUID> userIds;
    }
}

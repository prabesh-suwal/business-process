package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.TaskConfigurationDTO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Generic Assignment Resolver Service.
 * 
 * Centralized assignment resolution logic that works across all products (MMS,
 * LMS, etc.).
 * This replaces product-specific assignment services.
 * 
 * Supports:
 * - Multi-select format: roles[], departments[], users[]
 * - Legacy single-select format: type, role, scope
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentResolverService {

    private final TaskConfigurationService taskConfigService;

    /**
     * Resolve assignment for a task based on configuration.
     * 
     * @param processTemplateId The workflow template ID
     * @param taskKey           The BPMN task definition key
     * @param contextVariables  Process variables for dynamic resolution
     * @return AssignmentResult with candidateGroups and candidateUsers
     */
    public AssignmentResult resolveAssignment(
            UUID processTemplateId,
            String taskKey,
            Map<String, Object> contextVariables) {

        log.info("Resolving assignment for template {} task {}", processTemplateId, taskKey);

        // Get task configuration
        TaskConfigurationDTO taskConfig;
        try {
            taskConfig = taskConfigService.getTaskConfig(processTemplateId, taskKey);
        } catch (IllegalArgumentException e) {
            log.warn("No task config found for {} {}, using default", processTemplateId, taskKey);
            return defaultAssignment();
        }

        Map<String, Object> assignmentConfig = taskConfig.getAssignmentConfig();
        if (assignmentConfig == null || assignmentConfig.isEmpty()) {
            log.warn("No assignment config for task {}, using default", taskKey);
            return defaultAssignment();
        }

        // Try NEW multi-rule format first (rules array with OR logic)
        List<Map<String, Object>> rules = getListOfMapsFromConfig(assignmentConfig, "rules");
        if (!rules.isEmpty()) {
            log.info("Using multi-rule assignment format for task {} with {} rules", taskKey, rules.size());
            return resolveMultiRuleAssignment(rules, assignmentConfig);
        }

        // Try EXISTING multi-select format (roles[], departments[], users[] with AND
        // logic)
        List<String> roles = getListFromConfig(assignmentConfig, "roles");
        List<String> departments = getListFromConfig(assignmentConfig, "departments");
        List<String> users = getListFromConfig(assignmentConfig, "users");

        if (!roles.isEmpty() || !departments.isEmpty() || !users.isEmpty()) {
            log.info("Using multi-select assignment format for task {}", taskKey);
            return resolveMultiSelectAssignment(roles, departments, users);
        }

        // Fall back to LEGACY single-select format
        String assignmentType = (String) assignmentConfig.get("type");
        if (assignmentType != null) {
            log.info("Using legacy assignment format: {} for task {}", assignmentType, taskKey);
            return resolveLegacyAssignment(assignmentConfig, contextVariables);
        }

        log.warn("No valid assignment config for task {}, using default", taskKey);
        return defaultAssignment();
    }

    /**
     * Resolve using new multi-rule format.
     * Multiple rules are combined with OR logic.
     * Within each rule, criteria are combined with AND logic.
     */
    @SuppressWarnings("unchecked")
    private AssignmentResult resolveMultiRuleAssignment(
            List<Map<String, Object>> rules,
            Map<String, Object> assignmentConfig) {

        Set<String> candidateGroups = new HashSet<>();
        Set<String> candidateUsers = new HashSet<>();
        List<String> descriptions = new ArrayList<>();

        for (Map<String, Object> rule : rules) {
            String ruleName = (String) rule.getOrDefault("name", "Rule");
            Map<String, Object> criteria = (Map<String, Object>) rule.get("criteria");

            if (criteria == null) {
                continue;
            }

            // Each rule contributes to candidate groups/users (OR between rules)
            // Within each rule, all criteria identify the same pool (AND within rule)

            // Process regionIds, districtIds, stateIds - add as geo-based groups
            List<String> regionIds = getListFromConfig(criteria, "regionIds");
            if (!regionIds.isEmpty()) {
                for (String regionId : regionIds) {
                    candidateGroups.add("REGION_" + regionId);
                }
            }

            List<String> districtIds = getListFromConfig(criteria, "districtIds");
            if (!districtIds.isEmpty()) {
                for (String districtId : districtIds) {
                    candidateGroups.add("DISTRICT_" + districtId);
                }
            }

            List<String> stateIds = getListFromConfig(criteria, "stateIds");
            if (!stateIds.isEmpty()) {
                for (String stateId : stateIds) {
                    candidateGroups.add("STATE_" + stateId);
                }
            }

            // Process branchIds
            List<String> branchIds = getListFromConfig(criteria, "branchIds");
            if (!branchIds.isEmpty()) {
                for (String branchId : branchIds) {
                    candidateGroups.add("BRANCH_" + branchId);
                }
            }

            // Process departmentIds
            List<String> departmentIds = getListFromConfig(criteria, "departmentIds");
            if (!departmentIds.isEmpty()) {
                for (String deptId : departmentIds) {
                    candidateGroups.add("DEPT_" + deptId);
                }
            }

            // Process groupIds (committees)
            List<String> groupIds = getListFromConfig(criteria, "groupIds");
            if (!groupIds.isEmpty()) {
                for (String groupId : groupIds) {
                    candidateGroups.add("GROUP_" + groupId);
                }
            }

            // Process roleIds - use raw UUIDs (no prefix)
            List<String> roleIds = getListFromConfig(criteria, "roleIds");
            if (!roleIds.isEmpty()) {
                for (String roleId : roleIds) {
                    candidateGroups.add(roleId);
                }
            }

            // Process userIds - direct user assignment
            List<String> userIds = getListFromConfig(criteria, "userIds");
            if (!userIds.isEmpty()) {
                candidateUsers.addAll(userIds);
            }

            descriptions.add(ruleName);
        }

        // Add fallback role if specified
        String fallbackRoleId = (String) assignmentConfig.get("fallbackRoleId");
        if (fallbackRoleId != null && candidateGroups.isEmpty() && candidateUsers.isEmpty()) {
            candidateGroups.add(fallbackRoleId);
            descriptions.add("Fallback: " + fallbackRoleId);
        }

        return AssignmentResult.builder()
                .candidateGroups(candidateGroups.isEmpty() ? null : new ArrayList<>(candidateGroups))
                .candidateUsers(candidateUsers.isEmpty() ? null : new ArrayList<>(candidateUsers))
                .description("Rules: " + String.join(" | ", descriptions))
                .build();
    }

    /**
     * Resolve using existing multi-select format (roles[], departments[], users[]).
     */
    private AssignmentResult resolveMultiSelectAssignment(
            List<String> roles,
            List<String> departments,
            List<String> users) {

        List<String> candidateGroups = new ArrayList<>();
        List<String> candidateUsers = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();

        // Add role IDs as candidate groups (config should store role IDs directly)
        if (!roles.isEmpty()) {
            candidateGroups.addAll(roles);
            descriptions.add("Roles: " + String.join(", ", roles));
            log.info("Added {} role IDs to candidates", roles.size());
        }

        // Add departments as candidate groups (with DEPT_ prefix)
        if (!departments.isEmpty()) {
            for (String dept : departments) {
                candidateGroups.add("DEPT_" + dept.toUpperCase());
            }
            descriptions.add("Departments: " + String.join(", ", departments));
            log.info("Added {} departments to candidates", departments.size());
        }

        // Add specific users
        if (!users.isEmpty()) {
            candidateUsers.addAll(users);
            descriptions.add("Users: " + String.join(", ", users));
            log.info("Added {} users to candidates", users.size());
        }

        return AssignmentResult.builder()
                .candidateGroups(candidateGroups.isEmpty() ? null : candidateGroups)
                .candidateUsers(candidateUsers.isEmpty() ? null : candidateUsers)
                .description(String.join(" | ", descriptions))
                .build();
    }

    /**
     * Resolve using legacy single-select format.
     */
    private AssignmentResult resolveLegacyAssignment(
            Map<String, Object> config,
            Map<String, Object> contextVariables) {

        String type = ((String) config.get("type")).toUpperCase();

        return switch (type) {
            case "ROLE" -> {
                String roleId = resolveValue((String) config.get("role"), contextVariables);
                yield AssignmentResult.builder()
                        .candidateGroups(List.of(roleId))
                        .description("Role ID: " + roleId)
                        .build();
            }
            case "DEPARTMENT" -> {
                String dept = resolveValue((String) config.get("department"), contextVariables);
                String candidateGroup = "DEPT_" + dept.toUpperCase();
                yield AssignmentResult.builder()
                        .candidateGroups(List.of(candidateGroup))
                        .description("Department: " + dept)
                        .build();
            }
            case "GROUP", "COMMITTEE" -> {
                String groupCode = resolveValue((String) config.get("groupCode"), contextVariables);
                String candidateGroup = "COMMITTEE_" + groupCode.toUpperCase();
                yield AssignmentResult.builder()
                        .candidateGroups(List.of(candidateGroup))
                        .description("Committee: " + groupCode)
                        .build();
            }
            default -> defaultAssignment();
        };
    }

    /**
     * Default assignment when no config is found.
     */
    private AssignmentResult defaultAssignment() {
        return AssignmentResult.builder()
                .candidateGroups(List.of("BRANCH_MANAGER"))
                .description("Default: Branch Manager")
                .build();
    }

    /**
     * Resolve variable references like ${variableName}.
     */
    private String resolveValue(String value, Map<String, Object> vars) {
        if (value == null || vars == null)
            return value;
        if (!value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }
        String varName = value.substring(2, value.length() - 1);
        Object varValue = vars.get(varName);
        return varValue != null ? varValue.toString() : value;
    }

    /**
     * Safely get a list from config map.
     */
    @SuppressWarnings("unchecked")
    private List<String> getListFromConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }

    /**
     * Safely get a list of maps from config (for rules array).
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListOfMapsFromConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return Collections.emptyList();
    }

    /**
     * Result of assignment resolution.
     */
    @Data
    @Builder
    public static class AssignmentResult {
        private String assignee;
        private List<String> candidateGroups;
        private List<String> candidateUsers;
        private String description;
    }
}

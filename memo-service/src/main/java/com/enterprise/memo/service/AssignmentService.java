package com.enterprise.memo.service;

import com.enterprise.memo.client.CasClient;
import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.repository.MemoRepository;
import com.enterprise.memo.repository.MemoTopicRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Assignment resolution service in memo-service.
 * This is where business assignment logic lives - NOT in workflow-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final MemoRepository memoRepository;
    private final MemoTopicRepository topicRepository;
    private final com.enterprise.memo.repository.WorkflowStepConfigRepository workflowStepConfigRepository;
    private final CasClient casClient;

    /**
     * Resolve assignment for a task based on memo topic configuration.
     */
    public AssignmentResult resolveAssignment(UUID memoId, String taskDefinitionKey,
            Map<String, Object> processVariables) {
        log.info("Resolving assignment for memo {} task {}", memoId, taskDefinitionKey);

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));

        MemoTopic topic = memo.getTopic();

        // Load step config from WorkflowStepConfig table
        Optional<com.enterprise.memo.entity.WorkflowStepConfig> stepConfigOpt = workflowStepConfigRepository
                .findByMemoTopicIdAndTaskKey(topic.getId(), taskDefinitionKey);

        if (stepConfigOpt.isEmpty()) {
            log.warn("No step config found for topic {} task {}, using default",
                    topic.getCode(), taskDefinitionKey);
            return defaultAssignment(memo);
        }

        com.enterprise.memo.entity.WorkflowStepConfig stepConfig = stepConfigOpt.get();
        Map<String, Object> assignmentConfig = stepConfig.getAssignmentConfig();

        if (assignmentConfig == null || assignmentConfig.isEmpty()) {
            log.warn("No assignment config for task {}, using default", taskDefinitionKey);
            return defaultAssignment(memo);
        }

        // NEW MULTI-SELECT FORMAT - Check for roles[], departments[], users[] arrays
        List<String> roles = (List<String>) assignmentConfig.get("roles");
        List<String> departments = (List<String>) assignmentConfig.get("departments");
        List<String> users = (List<String>) assignmentConfig.get("users");

        if ((roles != null && !roles.isEmpty()) ||
                (departments != null && !departments.isEmpty()) ||
                (users != null && !users.isEmpty())) {
            log.info("Using NEW multi-select assignment format for task {}", taskDefinitionKey);
            return resolveMultiSelectAssignment(roles, departments, users, memo);
        }

        // LEGACY SINGLE-SELECT FORMAT - fallback for backward compatibility
        String assignmentType = (String) assignmentConfig.get("type");

        if (assignmentType == null) {
            log.warn("No assignment type specified for task {}, using default", taskDefinitionKey);
            return defaultAssignment(memo);
        }

        return switch (assignmentType.toUpperCase()) {
            case "ROLE" -> resolveByRole(assignmentConfig, processVariables, memo);
            case "AUTHORITY" -> resolveByAuthority(assignmentConfig, processVariables, memo);
            case "DEPARTMENT" -> resolveByDepartment(assignmentConfig, processVariables, memo);
            case "GROUP", "COMMITTEE" -> resolveByCommittee(assignmentConfig, processVariables, memo);
            default -> {
                log.warn("Unknown assignment type: {}, using default", assignmentType);

                yield defaultAssignment(memo);
            }

        };
    }

    /**
     * Resolve assignment using new multi-select format.
     * Combines roles, departments, and specific users into candidate lists.
     */
    private AssignmentResult resolveMultiSelectAssignment(
            List<String> roles,
            List<String> departments,
            List<String> users,
            Memo memo) {

        List<String> candidateGroups = new ArrayList<>();
        List<String> candidateUsers = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();

        // Add roles as candidate groups
        if (roles != null && !roles.isEmpty()) {
            candidateGroups.addAll(roles);
            descriptions.add("Roles: " + String.join(", ", roles));
            log.info("Added {} roles to candidates", roles.size());
        }

        // Add departments as candidate groups (with DEPT_ prefix)
        if (departments != null && !departments.isEmpty()) {
            for (String dept : departments) {
                candidateGroups.add("DEPT_" + dept.toUpperCase());
            }
            descriptions.add("Depts: " + String.join(", ", departments));
            log.info("Added {} departments to candidates", departments.size());
        }

        // Add specific users
        if (users != null && !users.isEmpty()) {
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

    private AssignmentResult resolveByRole(Map<String, Object> rule,
            Map<String, Object> vars, Memo memo) {
        String role = resolveValue((String) rule.get("role"), vars);
        String scope = (String) rule.get("scope");

        // Use role as-is (no ROLE_ prefix needed since JWT tokens have clean names)
        String candidateGroup = role;

        log.info("Resolved role assignment: {} at scope {}", candidateGroup, scope);

        return AssignmentResult.builder()
                .candidateGroups(List.of(candidateGroup))
                .description(role + " at " + formatScope(scope, vars))
                .build();
    }

    private AssignmentResult resolveByAuthority(Map<String, Object> rule,
            Map<String, Object> vars, Memo memo) {
        String limitField = (String) rule.get("limitField");
        Object limitValue = vars.get(limitField);

        if (limitValue == null) {
            return defaultAssignment(memo);
        }

        long amount = Long.parseLong(limitValue.toString().replaceAll("[^0-9]", ""));

        // Determine authority level based on amount
        String authorityRole = determineAuthorityRole(amount);
        String candidateGroup = "ROLE_" + authorityRole;

        return AssignmentResult.builder()
                .candidateGroups(List.of(candidateGroup))
                .description("Authority for amount " + amount)
                .build();
    }

    private AssignmentResult resolveByDepartment(Map<String, Object> rule,
            Map<String, Object> vars, Memo memo) {
        String department = resolveValue((String) rule.get("department"), vars);
        String group = (String) rule.get("group");

        String candidateGroup = "DEPT_" + department.toUpperCase().replace(" ", "_");
        if (group != null && !group.isBlank()) {
            candidateGroup += "_" + group.toUpperCase().replace(" ", "_");
        }

        return AssignmentResult.builder()
                .candidateGroups(List.of(candidateGroup))
                .description(department + (group != null ? " (" + group + ")" : ""))
                .build();
    }

    private AssignmentResult resolveByCommittee(Map<String, Object> rule,
            Map<String, Object> vars, Memo memo) {
        String committeeCode = resolveValue((String) rule.get("committeeCode"), vars);
        String candidateGroup = "COMMITTEE_" + committeeCode.toUpperCase();

        return AssignmentResult.builder()
                .candidateGroups(List.of(candidateGroup))
                .description("Committee " + committeeCode)
                .build();
    }

    private AssignmentResult defaultAssignment(Memo memo) {
        // Default: assign to initiator's manager or branch manager
        return AssignmentResult.builder()
                .candidateGroups(List.of("ROLE_BRANCH_MANAGER"))
                .description("Default: Branch Manager")
                .build();
    }

    private String resolveValue(String value, Map<String, Object> vars) {
        if (value == null || !value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }
        String varName = value.substring(2, value.length() - 1);
        Object varValue = vars.get(varName);
        return varValue != null ? varValue.toString() : value;
    }

    private String formatScope(String scope, Map<String, Object> vars) {
        if (scope == null)
            return "Any Location";
        return switch (scope) {
            case "ORIGINATING_BRANCH" -> "Originating Branch";
            case "CUSTOMER_BRANCH" -> "Customer's Branch";
            case "DISTRICT" -> "District Level";
            case "STATE" -> "State Level";
            case "HEAD_OFFICE" -> "Head Office";
            default -> "Any Location";
        };
    }

    private String determineAuthorityRole(long amount) {
        if (amount <= 500000) {
            return "BRANCH_MANAGER";
        } else if (amount <= 5000000) {
            return "CREDIT_COMMITTEE_A";
        } else if (amount <= 50000000) {
            return "CREDIT_COMMITTEE_B";
        } else {
            return "BOARD";
        }
    }

    @Data
    @Builder
    public static class AssignmentResult {
        private String assignee;
        private List<String> candidateGroups;
        private List<String> candidateUsers;
        private String description;
    }
}

package com.enterprise.workflow.service;

import com.enterprise.workflow.dto.ResolvedAssignment;
import com.enterprise.workflow.entity.WorkflowConfiguration;
import com.enterprise.workflow.repository.WorkflowConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Evaluates assignment rules against process variables to determine
 * who should be assigned to a task.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentRuleEvaluator {

    private final WorkflowConfigurationRepository configRepository;

    /**
     * Resolve the assignment for a task based on configuration and process
     * variables.
     *
     * @param configCode       The workflow configuration code
     * @param taskKey          The BPMN task key
     * @param processVariables Variables from the process instance
     * @return The resolved assignment
     */
    @SuppressWarnings("unchecked")
    public ResolvedAssignment resolveAssignment(String configCode, String taskKey,
            Map<String, Object> processVariables) {
        WorkflowConfiguration config = configRepository.findByCode(configCode)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configCode));

        Map<String, Object> rules = config.getAssignmentRules();
        if (rules == null || rules.isEmpty()) {
            log.warn("No assignment rules defined for config: {}", configCode);
            return null;
        }

        // Check for task-specific override
        Map<String, Object> taskOverrides = (Map<String, Object>) rules.get("taskOverrides");
        if (taskOverrides != null && taskOverrides.containsKey(taskKey)) {
            Map<String, Object> taskRule = (Map<String, Object>) taskOverrides.get(taskKey);
            ResolvedAssignment resolved = evaluateTaskRule(taskRule, processVariables);
            if (resolved != null) {
                log.debug("Resolved assignment for task {} in config {}: {}", taskKey, configCode, resolved);
                return resolved;
            }
        }

        // Fall back to default assignment
        Map<String, Object> defaultAssignment = (Map<String, Object>) rules.get("defaultAssignment");
        if (defaultAssignment != null) {
            return mapToResolvedAssignment(defaultAssignment, "default");
        }

        log.warn("No assignment could be resolved for task {} in config {}", taskKey, configCode);
        return null;
    }

    /**
     * Evaluate a task-specific rule which may contain conditions.
     */
    @SuppressWarnings("unchecked")
    private ResolvedAssignment evaluateTaskRule(Map<String, Object> taskRule, Map<String, Object> variables) {
        // Check if there are conditions to evaluate
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) taskRule.get("conditions");

        if (conditions != null && !conditions.isEmpty()) {
            for (Map<String, Object> condition : conditions) {
                if (evaluateCondition(condition, variables)) {
                    Map<String, Object> assignment = (Map<String, Object>) condition.get("assignment");
                    String conditionDesc = formatCondition(condition);
                    return mapToResolvedAssignment(assignment, conditionDesc);
                }
            }
        }

        // No conditions matched, use default for this task
        Map<String, Object> defaultAssignment = (Map<String, Object>) taskRule.get("default");
        if (defaultAssignment != null) {
            return mapToResolvedAssignment(defaultAssignment, "task-default");
        }

        // Try simple assignment (no conditions, just direct assignment)
        if (taskRule.containsKey("type")) {
            return mapToResolvedAssignment(taskRule, "direct");
        }

        return null;
    }

    /**
     * Evaluate a single condition against process variables.
     */
    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> variables) {
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object expectedValue = condition.get("value");

        Object actualValue = variables.get(field);

        if (actualValue == null) {
            log.debug("Field {} not found in variables, condition evaluates to false", field);
            return false;
        }

        try {
            return evaluateOperator(operator, actualValue, expectedValue);
        } catch (Exception e) {
            log.warn("Error evaluating condition: field={}, operator={}, expected={}, actual={}",
                    field, operator, expectedValue, actualValue, e);
            return false;
        }
    }

    /**
     * Evaluate an operator against actual and expected values.
     */
    private boolean evaluateOperator(String operator, Object actual, Object expected) {
        if (operator == null) {
            return false;
        }

        switch (operator.toUpperCase()) {
            case "EQUALS":
            case "EQ":
                return compareEquals(actual, expected);

            case "NOT_EQUALS":
            case "NE":
            case "NEQ":
                return !compareEquals(actual, expected);

            case "GREATER_THAN":
            case "GT":
                return compareNumbers(actual, expected) > 0;

            case "GREATER_THAN_OR_EQUALS":
            case "GTE":
            case "GE":
                return compareNumbers(actual, expected) >= 0;

            case "LESS_THAN":
            case "LT":
                return compareNumbers(actual, expected) < 0;

            case "LESS_THAN_OR_EQUALS":
            case "LTE":
            case "LE":
                return compareNumbers(actual, expected) <= 0;

            case "CONTAINS":
                return actual.toString().contains(expected.toString());

            case "STARTS_WITH":
                return actual.toString().startsWith(expected.toString());

            case "ENDS_WITH":
                return actual.toString().endsWith(expected.toString());

            case "IN":
                if (expected instanceof List) {
                    return ((List<?>) expected).contains(actual);
                }
                return false;

            case "NOT_IN":
                if (expected instanceof List) {
                    return !((List<?>) expected).contains(actual);
                }
                return true;

            case "IS_NULL":
                return actual == null;

            case "IS_NOT_NULL":
                return actual != null;

            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    private boolean compareEquals(Object actual, Object expected) {
        if (actual == null && expected == null)
            return true;
        if (actual == null || expected == null)
            return false;

        // Handle numeric comparison
        if (isNumeric(actual) && isNumeric(expected)) {
            return compareNumbers(actual, expected) == 0;
        }

        return actual.toString().equalsIgnoreCase(expected.toString());
    }

    private int compareNumbers(Object actual, Object expected) {
        BigDecimal actualNum = toBigDecimal(actual);
        BigDecimal expectedNum = toBigDecimal(expected);
        return actualNum.compareTo(expectedNum);
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number)
            return true;
        try {
            new BigDecimal(value.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        if (value instanceof Number)
            return new BigDecimal(value.toString());
        return new BigDecimal(value.toString());
    }

    @SuppressWarnings("unchecked")
    private ResolvedAssignment mapToResolvedAssignment(Map<String, Object> assignmentMap, String matchedCondition) {
        ResolvedAssignment assignment = new ResolvedAssignment();

        String typeStr = (String) assignmentMap.get("type");
        if (typeStr != null) {
            assignment.setType(ResolvedAssignment.AssignmentType.valueOf(typeStr.toUpperCase()));
        } else {
            assignment.setType(ResolvedAssignment.AssignmentType.ROLE);
        }

        // Support both "value" and "roleCode" for role type
        String value = (String) assignmentMap.get("value");
        if (value == null) {
            value = (String) assignmentMap.get("roleCode");
        }
        if (value == null) {
            value = (String) assignmentMap.get("userId");
        }
        assignment.setValue(value);

        Map<String, Object> scope = (Map<String, Object>) assignmentMap.get("scope");
        assignment.setScope(scope);

        assignment.setMatchedCondition(matchedCondition);

        return assignment;
    }

    private String formatCondition(Map<String, Object> condition) {
        return String.format("%s %s %s",
                condition.get("field"),
                condition.get("operator"),
                condition.get("value"));
    }
}

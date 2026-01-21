package com.enterprise.policyengine.engine;

import com.cas.common.policy.PolicyEvaluationRequest;
import com.cas.common.policy.PolicyEvaluationResponse;
import com.enterprise.policyengine.entity.*;
import com.enterprise.policyengine.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Core policy evaluation engine.
 * Evaluates authorization requests against active policies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEvaluator {

    private final PolicyRepository policyRepository;
    private final ExpressionResolver expressionResolver;
    private final TemporalEvaluator temporalEvaluator;

    /**
     * Evaluate an authorization request.
     * 
     * @param request The evaluation request with subject, resource, and action
     * @return Evaluation result with decision and details
     */
    public PolicyEvaluationResponse evaluate(PolicyEvaluationRequest request) {
        long startTime = System.currentTimeMillis();
        Map<String, PolicyEvaluationResponse.RuleGroupResult> details = new HashMap<>();

        // Find matching policies
        List<Policy> policies = policyRepository.findActiveByResourceTypeAndAction(
                request.getResource().getType(),
                request.getAction().getName(),
                request.getProduct());

        if (policies.isEmpty()) {
            log.debug("No policies found for resource={}, action={}",
                    request.getResource().getType(), request.getAction().getName());
            return PolicyEvaluationResponse.denied("No matching policy found",
                    System.currentTimeMillis() - startTime, details);
        }

        log.debug("Found {} policies for resource={}, action={}",
                policies.size(), request.getResource().getType(), request.getAction().getName());

        // Evaluate each policy (sorted by priority, highest first)
        for (Policy policy : policies) {
            PolicyEvaluationResult result = evaluatePolicy(policy, request);
            details.putAll(result.groupResults());

            if (result.matched()) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Policy '{}' matched with effect {}", policy.getName(), policy.getEffect());

                if (policy.getEffect() == PolicyEffect.ALLOW) {
                    return PolicyEvaluationResponse.allowed(
                            policy.getName(),
                            policy.getId(),
                            duration,
                            details);
                } else {
                    return PolicyEvaluationResponse.denied(
                            "Explicit deny by policy: " + policy.getName(),
                            duration,
                            details);
                }
            }
        }

        // No policy matched - default deny
        return PolicyEvaluationResponse.denied(
                "No policy conditions matched",
                System.currentTimeMillis() - startTime,
                details);
    }

    /**
     * Evaluate a single policy against the request.
     */
    private PolicyEvaluationResult evaluatePolicy(Policy policy, PolicyEvaluationRequest request) {
        Map<String, PolicyEvaluationResponse.RuleGroupResult> groupResults = new HashMap<>();

        // Group rules by ruleGroup
        Map<String, List<PolicyRule>> rulesByGroup = policy.getRules().stream()
                .sorted(Comparator.comparingInt(r -> r.getSortOrder() != null ? r.getSortOrder() : 0))
                .collect(Collectors.groupingBy(
                        r -> r.getRuleGroup() != null ? r.getRuleGroup() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()));

        // If no rules, policy matches
        if (rulesByGroup.isEmpty()) {
            return new PolicyEvaluationResult(true, groupResults);
        }

        // Evaluate each group (all groups must pass - AND logic between groups)
        for (Map.Entry<String, List<PolicyRule>> entry : rulesByGroup.entrySet()) {
            String groupName = entry.getKey();
            List<PolicyRule> groupRules = entry.getValue();

            boolean groupPassed = evaluateRuleGroup(groupRules, request);

            groupResults.put(groupName, PolicyEvaluationResponse.RuleGroupResult.builder()
                    .passed(groupPassed)
                    .message(groupPassed ? "All conditions met" : "Condition not satisfied")
                    .build());

            if (!groupPassed) {
                log.debug("Policy '{}' group '{}' did not pass", policy.getName(), groupName);
                return new PolicyEvaluationResult(false, groupResults);
            }
        }

        return new PolicyEvaluationResult(true, groupResults);
    }

    /**
     * Evaluate a group of rules (AND logic within group by default).
     */
    private boolean evaluateRuleGroup(List<PolicyRule> rules, PolicyEvaluationRequest request) {
        for (PolicyRule rule : rules) {
            if (!evaluateRule(rule, request)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate a single rule.
     */
    private boolean evaluateRule(PolicyRule rule, PolicyEvaluationRequest request) {
        // First check temporal condition (time-based access control)
        if (rule.hasTemporalCondition()) {
            if (!temporalEvaluator.evaluate(rule)) {
                log.debug("Rule '{}' failed temporal condition: {}",
                        rule.getAttribute(), rule.getTemporalCondition());
                return false;
            }
        }

        // Get the attribute value from the request
        Object attributeValue = expressionResolver.resolve(rule.getAttribute(), request);

        // Get the comparison value
        Object comparisonValue = resolveValue(rule, request);

        // Apply the operator
        boolean result = applyOperator(rule.getOperator(), attributeValue, comparisonValue);

        log.trace("Rule evaluation: {} {} {} = {}",
                rule.getAttribute(), rule.getOperator(), rule.getValue(), result);

        return result;
    }

    /**
     * Resolve the comparison value based on value type.
     */
    private Object resolveValue(PolicyRule rule, PolicyEvaluationRequest request) {
        if (rule.getValueType() == ValueType.EXPRESSION) {
            return expressionResolver.resolve(rule.getValue(), request);
        }

        return parseStaticValue(rule.getValue(), rule.getValueType());
    }

    /**
     * Parse a static value based on its type.
     */
    private Object parseStaticValue(String value, ValueType type) {
        return switch (type) {
            case STRING -> value;
            case NUMBER -> {
                try {
                    if (value.contains(".")) {
                        yield Double.parseDouble(value);
                    }
                    yield Long.parseLong(value);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            case BOOLEAN -> Boolean.parseBoolean(value);
            case ARRAY -> {
                // Simple comma-separated parsing
                if (value.startsWith("[") && value.endsWith("]")) {
                    value = value.substring(1, value.length() - 1);
                }
                yield Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
            case EXPRESSION -> value; // Should not happen
        };
    }

    /**
     * Apply an operator to compare values.
     */
    @SuppressWarnings("unchecked")
    private boolean applyOperator(Operator operator, Object attributeValue, Object comparisonValue) {
        return switch (operator) {
            case EQUALS -> Objects.equals(attributeValue, comparisonValue) ||
                    String.valueOf(attributeValue).equals(String.valueOf(comparisonValue));

            case NOT_EQUALS -> !Objects.equals(attributeValue, comparisonValue);

            case IN -> {
                List<Object> list = expressionResolver.toList(comparisonValue);
                yield list.stream().anyMatch(item -> Objects.equals(attributeValue, item) ||
                        String.valueOf(attributeValue).equals(String.valueOf(item)));
            }

            case NOT_IN -> {
                List<Object> list = expressionResolver.toList(comparisonValue);
                yield list.stream().noneMatch(item -> Objects.equals(attributeValue, item) ||
                        String.valueOf(attributeValue).equals(String.valueOf(item)));
            }

            case CONTAINS -> {
                List<Object> list = expressionResolver.toList(attributeValue);
                yield list.stream().anyMatch(item -> Objects.equals(item, comparisonValue) ||
                        String.valueOf(item).equals(String.valueOf(comparisonValue)));
            }

            case CONTAINS_ANY -> {
                List<Object> attrList = expressionResolver.toList(attributeValue);
                List<Object> compList = expressionResolver.toList(comparisonValue);
                yield attrList.stream().anyMatch(a -> compList.stream()
                        .anyMatch(c -> Objects.equals(a, c) || String.valueOf(a).equals(String.valueOf(c))));
            }

            case GREATER_THAN -> compareNumbers(attributeValue, comparisonValue) > 0;
            case GREATER_THAN_OR_EQUAL -> compareNumbers(attributeValue, comparisonValue) >= 0;
            case LESS_THAN -> compareNumbers(attributeValue, comparisonValue) < 0;
            case LESS_THAN_OR_EQUAL -> compareNumbers(attributeValue, comparisonValue) <= 0;

            case STARTS_WITH -> String.valueOf(attributeValue).startsWith(String.valueOf(comparisonValue));
            case ENDS_WITH -> String.valueOf(attributeValue).endsWith(String.valueOf(comparisonValue));
            case MATCHES_REGEX -> Pattern.compile(String.valueOf(comparisonValue))
                    .matcher(String.valueOf(attributeValue)).matches();

            case IS_NULL -> attributeValue == null;
            case IS_NOT_NULL -> attributeValue != null;

            case IS_TRUE ->
                Boolean.TRUE.equals(attributeValue) || "true".equalsIgnoreCase(String.valueOf(attributeValue));
            case IS_FALSE ->
                Boolean.FALSE.equals(attributeValue) || "false".equalsIgnoreCase(String.valueOf(attributeValue));
        };
    }

    /**
     * Compare two values as numbers.
     */
    private int compareNumbers(Object a, Object b) {
        Number numA = expressionResolver.toNumber(a);
        Number numB = expressionResolver.toNumber(b);

        if (numA == null || numB == null) {
            log.warn("Cannot compare non-numeric values: {} and {}", a, b);
            return -1; // Cannot compare, treat as less than
        }

        return Double.compare(numA.doubleValue(), numB.doubleValue());
    }

    /**
     * Result of evaluating a policy.
     */
    private record PolicyEvaluationResult(
            boolean matched,
            Map<String, PolicyEvaluationResponse.RuleGroupResult> groupResults) {
    }
}

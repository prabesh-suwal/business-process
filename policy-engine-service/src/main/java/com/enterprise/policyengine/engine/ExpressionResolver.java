package com.enterprise.policyengine.engine;

import com.cas.common.policy.PolicyEvaluationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Resolves expressions like "subject.branchIds" or "resource.amount"
 * from the evaluation request.
 */
@Slf4j
@Component
public class ExpressionResolver {

    /**
     * Resolve an expression path to its value from the evaluation request.
     * 
     * @param expression Path like "subject.permissions" or "resource.branchId"
     * @param request    The evaluation request containing subject and resource
     * @return The resolved value, or null if not found
     */
    public Object resolve(String expression, PolicyEvaluationRequest request) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        String[] parts = expression.split("\\.");

        if (parts.length < 2) {
            log.warn("Invalid expression format: {}", expression);
            return null;
        }

        // Get the root object
        Object current;
        switch (parts[0].toLowerCase()) {
            case "subject" -> current = request.getSubject();
            case "resource" -> current = request.getResource();
            case "environment", "context" -> current = request.getEnvironment();
            default -> {
                log.warn("Unknown expression root: {}", parts[0]);
                return null;
            }
        }

        if (current == null) {
            return null;
        }

        // Navigate the path
        for (int i = 1; i < parts.length; i++) {
            current = getProperty(current, parts[i]);
            if (current == null) {
                break;
            }
        }

        return current;
    }

    /**
     * Get a property value from an object (supports Map, POJO, and nested
     * attributes)
     */
    @SuppressWarnings("unchecked")
    private Object getProperty(Object obj, String property) {
        if (obj == null) {
            return null;
        }

        // Handle Map
        if (obj instanceof Map<?, ?> map) {
            // Try direct property
            if (map.containsKey(property)) {
                return map.get(property);
            }
            // Try camelCase to snake_case conversion
            String snakeCase = camelToSnake(property);
            if (map.containsKey(snakeCase)) {
                return map.get(snakeCase);
            }
            // Check 'attributes' sub-map
            if (map.containsKey("attributes") && map.get("attributes") instanceof Map) {
                Map<String, Object> attrs = (Map<String, Object>) map.get("attributes");
                if (attrs.containsKey(property)) {
                    return attrs.get(property);
                }
            }
            return null;
        }

        // Handle PolicyEvaluationRequest.Subject
        if (obj instanceof PolicyEvaluationRequest.Subject subject) {
            return switch (property) {
                case "id", "userId" -> subject.getUserId();
                case "username" -> subject.getUsername();
                case "roles" -> subject.getRoles();
                case "permissions" -> subject.getPermissions();
                case "branchIds" -> subject.getBranchIds();
                case "departmentIds" -> subject.getDepartmentIds();
                case "regionIds" -> subject.getRegionIds();
                case "approvalLimit" -> subject.getApprovalLimit();
                case "hierarchyLevel" -> subject.getHierarchyLevel();
                default -> {
                    // Check attributes map
                    if (subject.getAttributes() != null && subject.getAttributes().containsKey(property)) {
                        yield subject.getAttributes().get(property);
                    }
                    yield null;
                }
            };
        }

        // Handle PolicyEvaluationRequest.Resource
        if (obj instanceof PolicyEvaluationRequest.Resource resource) {
            return switch (property) {
                case "type" -> resource.getType();
                case "id" -> resource.getId();
                case "branchId" -> resource.getBranchId();
                case "regionId" -> resource.getRegionId();
                case "amount" -> resource.getAmount();
                case "ownerId" -> resource.getOwnerId();
                case "status" -> resource.getStatus();
                default -> {
                    // Check attributes map
                    if (resource.getAttributes() != null && resource.getAttributes().containsKey(property)) {
                        yield resource.getAttributes().get(property);
                    }
                    yield null;
                }
            };
        }

        // Handle generic POJOs via reflection
        try {
            PropertyDescriptor pd = new PropertyDescriptor(property, obj.getClass());
            return pd.getReadMethod().invoke(obj);
        } catch (Exception e) {
            log.debug("Could not resolve property '{}' on {}: {}", property, obj.getClass().getSimpleName(),
                    e.getMessage());
            return null;
        }
    }

    /**
     * Convert camelCase to snake_case
     */
    private String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Convert a value to a list for collection operations
     */
    @SuppressWarnings("unchecked")
    public List<Object> toList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        if (value instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }
        if (value.getClass().isArray()) {
            return List.of((Object[]) value);
        }
        return List.of(value);
    }

    /**
     * Convert a value to a number for comparison operations
     */
    public Number toNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String str) {
            try {
                if (str.contains(".")) {
                    return Double.parseDouble(str);
                }
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

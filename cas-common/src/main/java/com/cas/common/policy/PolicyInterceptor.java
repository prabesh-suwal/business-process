package com.cas.common.policy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.*;

/**
 * Spring HandlerInterceptor that enforces policy evaluation on API requests.
 * Extracts subject from JWT, builds action from request, calls PolicyClient.
 */
@Slf4j
@RequiredArgsConstructor
public class PolicyInterceptor implements HandlerInterceptor {

    private final PolicyClient policyClient;
    private final String serviceName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {

        // Skip non-controller handlers
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // Check for @RequiresPolicy annotation
        RequiresPolicy methodAnnotation = handlerMethod.getMethodAnnotation(RequiresPolicy.class);
        RequiresPolicy classAnnotation = handlerMethod.getBeanType().getAnnotation(RequiresPolicy.class);

        // Determine if we should skip
        if (methodAnnotation != null && methodAnnotation.skip()) {
            return true;
        }
        if (classAnnotation != null && classAnnotation.skip() && methodAnnotation == null) {
            return true;
        }

        // Get authentication from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // No authentication - let security handle this
            return true;
        }

        // Detect product code from annotation or header
        String productCode = detectProductCode(request, methodAnnotation, classAnnotation);

        // Build subject from authentication (with product-specific roles)
        PolicyEvaluationRequest.Subject subject = buildSubject(auth, request, productCode);

        // Build action from request
        PolicyEvaluationRequest.Action action = buildAction(request, methodAnnotation);

        // Build resource
        PolicyEvaluationRequest.Resource resource = buildResource(request, methodAnnotation, classAnnotation);

        // Build environment
        Map<String, Object> environment = buildEnvironment(request);

        // Create evaluation request
        PolicyEvaluationRequest evalRequest = PolicyEvaluationRequest.builder()
                .subject(subject)
                .action(action)
                .resource(resource)
                .product(productCode)
                .environment(environment)
                .build();

        log.debug("Evaluating policy: subject={}, action={}, resource={}, product={}",
                subject.getUserId(), action.getName(), resource.getType(), productCode);

        // Call policy engine
        PolicyEvaluationResponse evalResponse = policyClient.evaluate(evalRequest);

        if (!evalResponse.isAllowed()) {
            log.warn("Policy denied: user={}, action={}, resource={}, product={}, reason={}",
                    subject.getUserId(), action.getName(), resource.getType(), productCode, evalResponse.getReason());
            throw new AccessDeniedException("Access denied: " + evalResponse.getReason());
        }

        log.debug("Policy allowed: user={}, action={}, resource={}, product={}",
                subject.getUserId(), action.getName(), resource.getType(), productCode);
        return true;
    }

    /**
     * Detect product code from annotation or X-Product-Code header.
     */
    private String detectProductCode(HttpServletRequest request,
            RequiresPolicy methodAnnotation,
            RequiresPolicy classAnnotation) {
        // Priority 1: Method annotation
        if (methodAnnotation != null && !methodAnnotation.product().isEmpty()) {
            return methodAnnotation.product();
        }
        // Priority 2: Class annotation
        if (classAnnotation != null && !classAnnotation.product().isEmpty()) {
            return classAnnotation.product();
        }
        // Priority 3: X-Product-Code header
        String headerProduct = request.getHeader("X-Product-Code");
        if (headerProduct != null && !headerProduct.isBlank()) {
            return headerProduct;
        }
        // Default: use service name as product context
        return serviceName.toUpperCase();
    }

    private PolicyEvaluationRequest.Subject buildSubject(Authentication auth, HttpServletRequest request,
            String productCode) {
        Map<String, Object> attributes = new HashMap<>();

        // Extract claims from authentication if available
        Object principal = auth.getPrincipal();
        String userId = principal.toString();

        // Try to get product-specific roles from X-Product-Claims header
        List<String> roles = extractProductRoles(request, productCode);

        // Fallback to authorities if no product claims header
        if (roles.isEmpty()) {
            roles = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .toList();
        }

        // Get organization info from JWT claims if present (custom header or attribute)
        String branchId = request.getHeader("X-Branch-Id");
        String departmentId = request.getHeader("X-Department-Id");

        return PolicyEvaluationRequest.Subject.builder()
                .userId(userId)
                .roles(roles)
                .branchIds(branchId != null ? List.of(branchId) : new ArrayList<>())
                .departmentIds(departmentId != null ? List.of(departmentId) : new ArrayList<>())
                .attributes(attributes)
                .build();
    }

    /**
     * Extract roles for a specific product from X-Product-Claims header.
     * Header format: {"LMS": {"roles": ["MANAGER"], "scopes": [...]}, "WFM": {...}}
     */
    @SuppressWarnings("unchecked")
    private List<String> extractProductRoles(HttpServletRequest request, String productCode) {
        String productClaimsHeader = request.getHeader("X-Product-Claims");
        if (productClaimsHeader == null || productClaimsHeader.isBlank()) {
            return new ArrayList<>();
        }

        try {
            Map<String, Object> products = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(productClaimsHeader, Map.class);

            // Look for exact product match
            if (products.containsKey(productCode)) {
                Map<String, Object> productData = (Map<String, Object>) products.get(productCode);
                List<String> roles = (List<String>) productData.get("roles");
                return roles != null ? new ArrayList<>(roles) : new ArrayList<>();
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to parse X-Product-Claims header: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private PolicyEvaluationRequest.Action buildAction(HttpServletRequest request,
            RequiresPolicy annotation) {
        String httpMethod = request.getMethod();
        String actionName = mapHttpMethodToAction(httpMethod);

        // Override with annotation if provided
        if (annotation != null && !annotation.action().isEmpty()) {
            actionName = annotation.action();
        }

        return PolicyEvaluationRequest.Action.builder()
                .name(actionName)
                .httpMethod(httpMethod)
                .path(request.getRequestURI())
                .build();
    }

    private PolicyEvaluationRequest.Resource buildResource(HttpServletRequest request,
            RequiresPolicy methodAnnotation,
            RequiresPolicy classAnnotation) {
        String resourceType = serviceName.toUpperCase();

        // Try to infer from annotation
        if (methodAnnotation != null && !methodAnnotation.resource().isEmpty()) {
            resourceType = methodAnnotation.resource();
        } else if (classAnnotation != null && !classAnnotation.resource().isEmpty()) {
            resourceType = classAnnotation.resource();
        } else {
            // Try to infer from path
            String path = request.getRequestURI();
            resourceType = inferResourceType(path);
        }

        // Extract resource ID from path if present
        String resourceId = extractResourceId(request.getRequestURI());

        return PolicyEvaluationRequest.Resource.builder()
                .type(resourceType)
                .id(resourceId)
                .attributes(new HashMap<>())
                .build();
    }

    private Map<String, Object> buildEnvironment(HttpServletRequest request) {
        Map<String, Object> env = new HashMap<>();
        env.put("timestamp", Instant.now().toString());
        env.put("clientIp", request.getRemoteAddr());
        env.put("userAgent", request.getHeader("User-Agent"));
        env.put("service", serviceName);
        return env;
    }

    private String mapHttpMethodToAction(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> "READ";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> method;
        };
    }

    private String inferResourceType(String path) {
        // Extract from path like /api/branches/123 -> BRANCH
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (!part.isEmpty() && !part.matches("\\d+|[a-f0-9-]{36}")) {
                // Remove trailing 's' for plural
                String singular = part.endsWith("s") ? part.substring(0, part.length() - 1) : part;
                return singular.toUpperCase();
            }
        }
        return "UNKNOWN";
    }

    private String extractResourceId(String path) {
        // Extract UUID or numeric ID from path
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (part.matches("\\d+|[a-f0-9-]{36}")) {
                return part;
            }
        }
        return null;
    }
}

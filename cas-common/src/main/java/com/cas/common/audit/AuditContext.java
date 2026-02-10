package com.cas.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to gather audit context information from the current request.
 * Extracts actor details from SecurityContext and request metadata.
 */
public final class AuditContext {

    private AuditContext() {
    }

    /**
     * Get the current actor ID from SecurityContext.
     */
    public static String getActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    /**
     * Get the actor type based on authentication.
     */
    public static ActorType getActorType() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ActorType.ANONYMOUS;
        }

        // Check if it's an API client (typically has client_credentials grant)
        if (auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("SCOPE_client"))) {
            return ActorType.API_CLIENT;
        }

        return ActorType.USER;
    }

    /**
     * Get the current actor's roles.
     */
    public static List<String> getActorRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Get the client IP address from the current request.
     */
    public static String getIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        // Check for forwarded header first (when behind proxy/gateway)
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Take the first IP in the chain
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Get actor name from request header (set by gateway).
     */
    public static String getActorName() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return request.getHeader("X-User-Name");
        }
        return null;
    }

    /**
     * Get actor email from request header (set by gateway).
     */
    public static String getActorEmail() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return request.getHeader("X-User-Email");
        }
        return null;
    }

    /**
     * Get product code from X-Product-Code header (set by gateway).
     * This extracts the product code dynamically from the request,
     * allowing the same service to handle multiple products.
     */
    public static String getProductCode() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String productCode = request.getHeader("X-Product-Code");
            if (productCode != null && !productCode.isBlank()) {
                // Handle comma-separated product codes (take first)
                return productCode.split(",")[0].trim();
            }
        }
        return null;
    }

    /**
     * Get correlation ID from MDC (set by CorrelationIdFilter).
     */
    public static String getCorrelationId() {
        return org.slf4j.MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
    }

    /**
     * Get the current HTTP request.
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

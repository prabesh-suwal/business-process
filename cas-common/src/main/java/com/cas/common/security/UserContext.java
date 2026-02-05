package com.cas.common.security;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;

/**
 * Immutable context holding current user/client details from gateway headers.
 * Populated by UserContextFilter and accessible via UserContextHolder.
 */
@Getter
@Builder
public class UserContext {

    /**
     * User or client ID from token subject (UUID format).
     */
    private final String userId;

    /**
     * User email address (null for service tokens).
     */
    private final String email;

    /**
     * User display name (null for service tokens).
     */
    private final String name;

    /**
     * Token type: "USER" or "SERVICE".
     */
    private final String tokenType;

    /**
     * Role codes for the current product (e.g., "MAKER", "CHECKER").
     */
    @Builder.Default
    private final Set<String> roles = Collections.emptySet();

    /**
     * Role UUIDs for the current product (for assignment matching).
     */
    @Builder.Default
    private final Set<String> roleIds = Collections.emptySet();

    /**
     * Granted scopes (e.g., "memo:read", "memo:write").
     */
    @Builder.Default
    private final Set<String> scopes = Collections.emptySet();

    /**
     * Product code for the current request (e.g., "MMS", "LMS").
     */
    private final String productCode;

    /**
     * Product ID (UUID) for the current request.
     */
    private final String productId;

    /**
     * User's branch ID (from org claims).
     */
    private final String branchId;

    /**
     * User's department ID (from org claims).
     */
    private final String departmentId;

    /**
     * JWT ID for correlation/audit.
     */
    private final String tokenJti;

    /**
     * Client name (for service tokens).
     */
    private final String clientName;

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles.
     */
    public boolean hasAnyRole(String... checkRoles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String role : checkRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has a specific scope.
     */
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }

    /**
     * Check if this is a service/client token (not a user token).
     */
    public boolean isServiceToken() {
        return "SERVICE".equals(tokenType);
    }

    /**
     * Check if this is a user token.
     */
    public boolean isUserToken() {
        return "USER".equals(tokenType);
    }

    /**
     * Check if user context is populated (userId is present).
     */
    public boolean isAuthenticated() {
        return userId != null && !userId.isEmpty();
    }

    /**
     * Get userId as string, never null (returns empty string if not set).
     */
    public String getUserIdOrEmpty() {
        return userId != null ? userId : "";
    }

    /**
     * Create an empty/anonymous context.
     */
    public static UserContext anonymous() {
        return UserContext.builder().build();
    }
}

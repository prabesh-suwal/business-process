package com.cas.common.security;

/**
 * Constants for CAS header names used between gateway and services.
 */
public final class CasHeaders {

    private CasHeaders() {
        // Utility class
    }

    /**
     * User/Client ID from token subject.
     */
    public static final String USER_ID = "X-User-Id";

    /**
     * User email (for user tokens only).
     */
    public static final String USER_EMAIL = "X-User-Email";

    /**
     * User display name.
     */
    public static final String USER_NAME = "X-User-Name";

    /**
     * Token type: USER or SERVICE.
     */
    public static final String TOKEN_TYPE = "X-Token-Type";

    /**
     * Comma-separated list of scopes.
     */
    public static final String SCOPES = "X-Scopes";

    /**
     * Product code the request is for.
     */
    public static final String PRODUCT_CODE = "X-Product-Code";

    /**
     * Comma-separated list of roles for the product.
     */
    public static final String ROLES = "X-Roles";

    /**
     * Original client IP (for audit).
     */
    public static final String CLIENT_IP = "X-Client-IP";

    /**
     * JWT ID for correlation.
     */
    public static final String TOKEN_JTI = "X-Token-Jti";

    /**
     * API client name (for service tokens).
     */
    public static final String CLIENT_NAME = "X-Client-Name";

    // ===== Role Identity =====

    /**
     * Comma-separated list of role IDs (UUIDs) for the product.
     */
    public static final String ROLE_IDS = "X-Role-Ids";

    /**
     * Backward compatible roles header for workflow-service.
     */
    public static final String USER_ROLES = "X-User-Roles";

    // ===== Product/Org Headers =====

    /**
     * Product ID (UUID).
     */
    public static final String PRODUCT_ID = "X-Product-Id";

    /**
     * Branch ID for multi-branch organizations.
     */
    public static final String BRANCH_ID = "X-Branch-Id";

    /**
     * Department ID for organizational hierarchy.
     */
    public static final String DEPARTMENT_ID = "X-Department-Id";

    // ===== Service Identity (for service-to-service calls) =====

    /**
     * Name of the calling service.
     */
    public static final String SERVICE_NAME = "X-CAS-Service-Name";

    /**
     * Service authentication token/credential.
     */
    public static final String SERVICE_TOKEN = "X-CAS-Service-Token";

    /**
     * Actor type: USER, SERVICE, or SYSTEM.
     */
    public static final String ACTOR_TYPE = "X-CAS-Actor-Type";

    // ===== Actor Type Values =====

    public static final String ACTOR_TYPE_USER = "USER";
    public static final String ACTOR_TYPE_SERVICE = "SERVICE";
    public static final String ACTOR_TYPE_SYSTEM = "SYSTEM";
}

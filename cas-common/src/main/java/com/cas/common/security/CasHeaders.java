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
}

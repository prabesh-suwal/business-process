package com.cas.common.exception;

/**
 * Exception for insufficient scope/permission.
 */
public class InsufficientScopeException extends CasException {

    private final String requiredScope;

    public InsufficientScopeException(String requiredScope) {
        super("insufficient_scope", "Required scope: " + requiredScope, 403);
        this.requiredScope = requiredScope;
    }

    public String getRequiredScope() {
        return requiredScope;
    }
}

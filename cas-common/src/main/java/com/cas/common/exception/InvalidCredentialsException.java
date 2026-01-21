package com.cas.common.exception;

/**
 * Exception for invalid credentials during authentication.
 */
public class InvalidCredentialsException extends CasException {

    public InvalidCredentialsException() {
        super("invalid_grant", "Invalid username or password", 401);
    }

    public InvalidCredentialsException(String message) {
        super("invalid_grant", message, 401);
    }
}

package com.cas.common.exception;

/**
 * Exception for invalid or expired tokens.
 */
public class InvalidTokenException extends CasException {

    public InvalidTokenException() {
        super("invalid_token", "Token is invalid or expired", 401);
    }

    public InvalidTokenException(String message) {
        super("invalid_token", message, 401);
    }
}

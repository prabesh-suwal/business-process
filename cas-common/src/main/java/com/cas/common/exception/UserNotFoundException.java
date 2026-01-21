package com.cas.common.exception;

/**
 * Exception for user/client not found.
 */
public class UserNotFoundException extends CasException {

    public UserNotFoundException() {
        super("user_not_found", "User not found", 404);
    }

    public UserNotFoundException(String userId) {
        super("user_not_found", "User not found: " + userId, 404);
    }
}

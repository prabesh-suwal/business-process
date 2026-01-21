package com.cas.common.exception;

/**
 * Exception for account locked/disabled states.
 */
public class AccountLockedException extends CasException {

    public AccountLockedException() {
        super("account_locked", "Account is locked or disabled", 403);
    }

    public AccountLockedException(String message) {
        super("account_locked", message, 403);
    }
}

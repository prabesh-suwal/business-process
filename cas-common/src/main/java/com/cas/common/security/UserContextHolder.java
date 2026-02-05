package com.cas.common.security;

import java.util.Optional;

/**
 * ThreadLocal-based holder for UserContext.
 * Similar pattern to Spring's SecurityContextHolder.
 * 
 * Usage:
 * UserContext user = UserContextHolder.require();
 * String userId = user.getUserId();
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    private UserContextHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the current UserContext, or null if not set.
     */
    public static UserContext getContext() {
        return contextHolder.get();
    }

    /**
     * Set the UserContext for the current thread.
     */
    public static void setContext(UserContext context) {
        contextHolder.set(context);
    }

    /**
     * Clear the UserContext. Should be called in filter's finally block.
     */
    public static void clear() {
        contextHolder.remove();
    }

    /**
     * Get the current UserContext as Optional.
     */
    public static Optional<UserContext> current() {
        return Optional.ofNullable(contextHolder.get());
    }

    /**
     * Get the current UserContext, throwing if not present.
     * Use this when authentication is required.
     * 
     * @throws IllegalStateException if no UserContext is set
     */
    public static UserContext require() {
        UserContext context = contextHolder.get();
        if (context == null || !context.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user context available. " +
                    "Ensure UserContextFilter is configured and request passed through gateway.");
        }
        return context;
    }

    /**
     * Get the current UserContext, or an anonymous context if not set.
     */
    public static UserContext getOrAnonymous() {
        UserContext context = contextHolder.get();
        return context != null ? context : UserContext.anonymous();
    }

    /**
     * Get the current user ID, or null if not authenticated.
     */
    public static String getUserId() {
        UserContext context = contextHolder.get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * Check if a user context is present and authenticated.
     */
    public static boolean isAuthenticated() {
        UserContext context = contextHolder.get();
        return context != null && context.isAuthenticated();
    }
}

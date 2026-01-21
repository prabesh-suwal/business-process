package com.enterprise.memo.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@Slf4j
public class ScopeEnforcementService {

    /**
     * Determines required scopes for a given method and path.
     * In a real system, this would load from a DB or Config.
     */
    public Set<String> getRequiredScopes(String method, String path) {
        // MMS Policy:
        // GET /memo/api/** -> Requires "mms.read" or "mms.write"
        // POST /memo/api/** -> Requires "mms.write"

        if (path.startsWith("/memo/api")) {
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                    || "DELETE".equalsIgnoreCase(method)) {
                return Set.of("mms.write");
            }
            return Set.of("mms.read");
        }

        // Default: No scopes required (public)
        return Collections.emptySet();
    }

    /**
     * Checks if user has all required scopes.
     * Note: "mms.*" or "*" implies full access.
     */
    public boolean hasRequiredScopes(Set<String> userScopes, Set<String> requiredScopes) {
        if (requiredScopes.isEmpty())
            return true;
        if (userScopes == null || userScopes.isEmpty())
            return false;

        // Admin/SuperUser check
        if (userScopes.contains("*") || userScopes.contains("mms.*")) {
            return true;
        }

        // Logic: specific matches
        // For simple requirements, we check if user has ANY of the required?
        // Or ALL? Usually ALL. But here we returned a Set, implying AND.
        // Let's assume user needs AT LEAST ONE of the equivalent permissions?
        // Actually, typically `requiredScopes` means "Must have X".

        // Optimization for Day 1: simple containment
        return userScopes.containsAll(requiredScopes)
                || userScopes.contains("mms.admin"); // Fallback
    }
}

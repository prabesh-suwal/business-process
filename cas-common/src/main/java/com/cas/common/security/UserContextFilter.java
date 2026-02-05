package com.cas.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Servlet filter that extracts user information from gateway-forwarded headers
 * and populates the UserContextHolder for the current request thread.
 * 
 * This filter should run after authentication but before request processing.
 * Add this filter to your SecurityConfig or register it as a bean.
 * 
 * Example usage in SecurityConfig:
 * http.addFilterAfter(new UserContextFilter(),
 * UsernamePasswordAuthenticationFilter.class);
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            UserContext context = extractUserContext(request);
            UserContextHolder.setContext(context);

            if (context.isAuthenticated()) {
                log.debug("UserContext set for user: {} ({})", context.getUserId(), context.getEmail());
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    /**
     * Extract user context from request headers.
     */
    private UserContext extractUserContext(HttpServletRequest request) {
        String userId = getHeader(request, CasHeaders.USER_ID);
        String email = getHeader(request, CasHeaders.USER_EMAIL);
        String name = getHeader(request, CasHeaders.USER_NAME);
        String tokenType = getHeader(request, CasHeaders.TOKEN_TYPE);
        String productCode = getHeader(request, CasHeaders.PRODUCT_CODE);
        String tokenJti = getHeader(request, CasHeaders.TOKEN_JTI);
        String clientName = getHeader(request, CasHeaders.CLIENT_NAME);

        // Parse comma-separated roles
        Set<String> roles = parseCommaSeparated(getHeader(request, CasHeaders.ROLES));

        // Parse comma-separated scopes
        Set<String> scopes = parseCommaSeparated(getHeader(request, CasHeaders.SCOPES));

        // Get product ID from header (if available)
        String productId = getHeader(request, "X-Product-Id");

        // Get role IDs from header (if available)
        Set<String> roleIds = parseCommaSeparated(getHeader(request, "X-Role-Ids"));

        // Get org info from attributes (set by JwtAuthFilter) or headers
        String branchId = getAttributeOrHeader(request, "branchId", "X-Branch-Id");
        String departmentId = getAttributeOrHeader(request, "departmentId", "X-Department-Id");

        return UserContext.builder()
                .userId(userId)
                .email(email)
                .name(name)
                .tokenType(tokenType)
                .roles(roles)
                .roleIds(roleIds)
                .scopes(scopes)
                .productCode(productCode)
                .productId(productId)
                .branchId(branchId)
                .departmentId(departmentId)
                .tokenJti(tokenJti)
                .clientName(clientName)
                .build();
    }

    private String getHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return (value != null && !value.isEmpty()) ? value : null;
    }

    private String getAttributeOrHeader(HttpServletRequest request, String attributeName, String headerName) {
        // First try request attribute (set by JwtAuthFilter from JWT claims)
        Object attr = request.getAttribute(attributeName);
        if (attr != null) {
            return attr.toString();
        }
        // Fall back to header
        return getHeader(request, headerName);
    }

    private Set<String> parseCommaSeparated(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptySet();
        }
        String[] parts = value.split(",");
        Set<String> result = new HashSet<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return Collections.unmodifiableSet(result);
    }
}

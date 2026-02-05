package com.cas.common.webclient;

import com.cas.common.security.CasHeaders;
import com.cas.common.security.UserContext;
import com.cas.common.security.UserContextHolder;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebClient ExchangeFilterFunction that automatically propagates UserContext
 * headers and service identity for service-to-service calls.
 * 
 * Security model:
 * - Service identity is ALWAYS required (never absent)
 * - User context is optional (may be absent for async jobs, scheduled tasks)
 * 
 * Usage:
 * 
 * <pre>
 * UserContextWebClientFilter filter = new UserContextWebClientFilter("memo-service", serviceToken);
 * WebClient client = WebClient.builder().filter(filter).build();
 * </pre>
 */
@Slf4j
public class UserContextWebClientFilter implements ExchangeFilterFunction {

    private final String serviceName;
    private final String serviceToken;

    /**
     * Create a filter with mandatory service identity.
     * 
     * @param serviceName  The name of the calling service (e.g., "memo-service")
     * @param serviceToken Optional service authentication token/credential
     */
    public UserContextWebClientFilter(String serviceName, String serviceToken) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName must not be null or blank - service identity is required");
        }
        this.serviceName = serviceName;
        this.serviceToken = serviceToken;
    }

    /**
     * Create a filter with service name only (no token).
     */
    public UserContextWebClientFilter(String serviceName) {
        this(serviceName, null);
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        ClientRequest.Builder builder = ClientRequest.from(request);

        // ===== Service Identity (ALWAYS required) =====
        builder.header(CasHeaders.SERVICE_NAME, serviceName);
        if (serviceToken != null && !serviceToken.isBlank()) {
            builder.header(CasHeaders.SERVICE_TOKEN, serviceToken);
        }

        // ===== User Context (optional) =====
        UserContext ctx = UserContextHolder.getContext();

        if (ctx != null && ctx.isAuthenticated()) {
            // Actor type is USER when we have user context
            builder.header(CasHeaders.ACTOR_TYPE, CasHeaders.ACTOR_TYPE_USER);

            // User identity
            if (ctx.getUserId() != null) {
                builder.header(CasHeaders.USER_ID, ctx.getUserId());
            }
            if (ctx.getEmail() != null) {
                builder.header(CasHeaders.USER_EMAIL, ctx.getEmail());
            }
            if (ctx.getName() != null) {
                builder.header(CasHeaders.USER_NAME, ctx.getName());
            }

            // Roles (both names and IDs)
            if (ctx.getRoles() != null && !ctx.getRoles().isEmpty()) {
                String rolesStr = String.join(",", ctx.getRoles());
                builder.header(CasHeaders.ROLES, rolesStr);
                builder.header(CasHeaders.USER_ROLES, rolesStr); // backward compat
            }
            if (ctx.getRoleIds() != null && !ctx.getRoleIds().isEmpty()) {
                builder.header(CasHeaders.ROLE_IDS, String.join(",", ctx.getRoleIds()));
            }

            // Scopes
            if (ctx.getScopes() != null && !ctx.getScopes().isEmpty()) {
                builder.header(CasHeaders.SCOPES, String.join(",", ctx.getScopes()));
            }

            // Product/Org context
            if (ctx.getProductCode() != null) {
                builder.header(CasHeaders.PRODUCT_CODE, ctx.getProductCode());
            }
            if (ctx.getProductId() != null) {
                builder.header(CasHeaders.PRODUCT_ID, ctx.getProductId());
            }
            if (ctx.getBranchId() != null) {
                builder.header(CasHeaders.BRANCH_ID, ctx.getBranchId());
            }
            if (ctx.getDepartmentId() != null) {
                builder.header(CasHeaders.DEPARTMENT_ID, ctx.getDepartmentId());
            }

            // Token metadata
            if (ctx.getTokenType() != null) {
                builder.header(CasHeaders.TOKEN_TYPE, ctx.getTokenType());
            }
            if (ctx.getTokenJti() != null) {
                builder.header(CasHeaders.TOKEN_JTI, ctx.getTokenJti());
            }
            if (ctx.getClientName() != null) {
                builder.header(CasHeaders.CLIENT_NAME, ctx.getClientName());
            }

            log.debug("Propagating user context for user {} via service {}", ctx.getUserId(), serviceName);
        } else {
            // Actor type is SERVICE when no user context (async jobs, scheduled tasks,
            // etc.)
            builder.header(CasHeaders.ACTOR_TYPE, CasHeaders.ACTOR_TYPE_SERVICE);
            log.debug("No user context - making service-to-service call as {}", serviceName);
        }

        return next.exchange(builder.build());
    }

    /**
     * Get the service name this filter is configured with.
     */
    public String getServiceName() {
        return serviceName;
    }
}

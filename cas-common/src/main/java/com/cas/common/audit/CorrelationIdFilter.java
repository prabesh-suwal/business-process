package com.cas.common.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that manages correlation IDs for cross-service request
 * tracing.
 * 
 * If a correlation ID is present in the incoming request header, it is
 * propagated.
 * Otherwise, a new correlation ID is generated.
 * 
 * The correlation ID is stored in MDC for logging and in the response header.
 * 
 * This bean is registered by {@link AuditServletAutoConfiguration}.
 * Do NOT add @Component — it is managed by explicit @Bean definition.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Store in MDC for logging and audit events
        MDC.put(CORRELATION_ID_KEY, correlationId);

        // Add to response header for downstream services
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.remove(CORRELATION_ID_KEY);
        }
    }
}

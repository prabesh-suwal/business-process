package com.cas.common.dto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for unified API response wrapping.
 *
 * <p>
 * Activates only for Servlet-based web applications (not reactive/gateway).
 * Registers:
 * <ul>
 * <li>{@link ApiResponseAdvice} — auto-wraps all controller responses in
 * {@link ApiResponse}</li>
 * <li>{@link DefaultGlobalExceptionHandler} — catches exceptions and returns
 * {@link ApiResponse} errors</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({ ApiResponseAdvice.class, DefaultGlobalExceptionHandler.class })
public class ApiResponseAutoConfiguration {
}

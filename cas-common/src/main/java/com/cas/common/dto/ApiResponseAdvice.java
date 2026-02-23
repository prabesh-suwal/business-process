package com.cas.common.dto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Automatically wraps all REST controller responses in {@link ApiResponse}.
 *
 * <p>
 * Skip behavior:
 * <ul>
 * <li>Body is already {@code ApiResponse} → no wrapping</li>
 * <li>Body is {@code ErrorResponse} → no wrapping (legacy support)</li>
 * <li>Body is {@code String} → no wrapping (Spring MVC StringConverter
 * issue)</li>
 * <li>Method or class annotated with {@code @RawResponse} → no wrapping</li>
 * </ul>
 *
 * <p>
 * Message resolution order:
 * <ol>
 * <li>{@code @ApiMessage("Custom message")} annotation on the method</li>
 * <li>Default based on HTTP method: GET → "Retrieved successfully", POST →
 * "Created successfully", etc.</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {

        // Skip if the controller or method is annotated with @RawResponse
        if (returnType.hasMethodAnnotation(RawResponse.class)) {
            return false;
        }
        if (returnType.getDeclaringClass().isAnnotationPresent(RawResponse.class)) {
            return false;
        }

        // Skip SpringDoc OpenAPI endpoints
        if (returnType.getDeclaringClass().getName().contains("org.springdoc")) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        // Don't double-wrap
        if (body instanceof ApiResponse) {
            return body;
        }

        // Don't wrap legacy ErrorResponse
        if (body instanceof ErrorResponse) {
            return body;
        }

        // Don't wrap Strings (Spring MVC uses StringHttpMessageConverter which can't
        // serialize objects)
        if (body instanceof String) {
            return body;
        }

        // Don't wrap null responses (void endpoints return null body)
        if (body == null) {
            String message = resolveMessage(returnType, request);
            return ApiResponse.ok(message);
        }

        String message = resolveMessage(returnType, request);
        return ApiResponse.success(body, message);
    }

    /**
     * Resolve the response message from @ApiMessage annotation or HTTP method
     * default.
     */
    private String resolveMessage(MethodParameter returnType, ServerHttpRequest request) {
        // Check for @ApiMessage annotation
        ApiMessage annotation = returnType.getMethodAnnotation(ApiMessage.class);
        if (annotation != null) {
            return annotation.value();
        }

        // Default based on HTTP method
        String method = request.getMethod().name();
        return switch (method) {
            case "GET" -> "Retrieved successfully";
            case "POST" -> "Created successfully";
            case "PUT", "PATCH" -> "Updated successfully";
            case "DELETE" -> "Deleted successfully";
            default -> "Operation completed successfully";
        };
    }
}

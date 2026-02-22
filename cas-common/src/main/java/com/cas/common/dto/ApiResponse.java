package com.cas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Universal API response envelope.
 * All API endpoints should return this wrapper for consistent response shape.
 *
 * <p>
 * Success:
 * 
 * <pre>
 * {
 *   "success": true,
 *   "message": "Tasks retrieved",
 *   "data": { ... },
 *   "timestamp": "2026-02-21T13:30:00Z"
 * }
 * </pre>
 *
 * <p>
 * Error:
 * 
 * <pre>
 * {
 *   "success": false,
 *   "message": "Validation failed",
 *   "errors": [{"field": "subject", "message": "must not be blank"}],
 *   "timestamp": "2026-02-21T13:30:00Z"
 * }
 * </pre>
 *
 * @param <T> the type of the response data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private List<FieldError> errors;
    private Instant timestamp;

    // ── Success factories ──────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiResponse<Void> ok() {
        return ApiResponse.<Void>builder()
                .success(true)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiResponse<Void> ok(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    // ── Error factories ────────────────────────────────────────

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> validationError(List<FieldError> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message("Validation failed")
                .errors(errors)
                .timestamp(Instant.now())
                .build();
    }
}

package com.cas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured validation error detail.
 * Used within {@link ApiResponse#getErrors()} for field-level validation
 * failures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldError {

    /** The field that failed validation. */
    private String field;

    /** Human-readable error message. */
    private String message;

    /** The value that was rejected (optional). */
    private Object rejectedValue;

    public static FieldError of(String field, String message) {
        return new FieldError(field, message, null);
    }
}

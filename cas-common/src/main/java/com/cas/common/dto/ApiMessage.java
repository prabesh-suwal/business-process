package com.cas.common.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide a meaningful success message for the auto-wrapped
 * {@link ApiResponse}.
 * <p>
 * Supports placeholders:
 * <ul>
 * <li>{@code {entity}} — entity name (inferred from return type if not
 * set)</li>
 * </ul>
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * &#64;GetMapping("/inbox")
 * &#64;ApiMessage("Tasks retrieved successfully")
 * public PagedData&lt;TaskDTO&gt; getInbox() { ... }
 * </pre>
 *
 * <p>
 * If not present, the advice generates a default message from the HTTP method:
 * GET → "Retrieved successfully", POST → "Created successfully", etc.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMessage {
    String value();
}

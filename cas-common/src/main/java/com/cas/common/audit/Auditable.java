package com.cas.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable automatic audit logging for a method.
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 * &#64;Auditable(
 *     action = AuditAction.CREATE,
 *     category = AuditCategory.DATA_ACCESS,
 *     resourceType = "MEMO",
 *     resourceIdExpression = "#result.id"
 * )
 * public Memo createMemo(MemoDTO dto) { ... }
 * }
 * </pre>
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The action being performed.
     */
    AuditAction action();

    /**
     * Category of the action. Defaults to DATA_ACCESS.
     */
    AuditCategory category() default AuditCategory.DATA_ACCESS;

    /**
     * Type of resource being acted upon (e.g., "MEMO", "USER", "POLICY").
     */
    String resourceType();

    /**
     * SpEL expression to extract the resource ID.
     * 
     * Available variables:
     * - #result: the return value of the method
     * - #args: array of method arguments
     * - #arg0, #arg1, etc.: individual arguments
     * - #p0, #p1, etc.: alias for arguments
     * 
     * Examples:
     * - "#result.id" - ID from return value
     * - "#arg0" - first argument is the ID
     * - "#args[0].id" - ID from first argument object
     */
    String resourceIdExpression() default "";

    /**
     * Human-readable description of the action.
     * Can include SpEL expressions wrapped in #{...}
     */
    String description() default "";

    /**
     * Product code for this action. If empty, uses default from properties.
     */
    String productCode() default "";
}

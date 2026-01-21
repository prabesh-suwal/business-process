package com.cas.common.policy;

import java.lang.annotation.*;

/**
 * Annotation to mark endpoints that require policy evaluation.
 * Applied to controller methods to enforce authorization.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPolicy {

    /**
     * The resource type being accessed (e.g., "BRANCH", "LOAN", "USER").
     * If empty, will be inferred from path.
     */
    String resource() default "";

    /**
     * The action being performed. If empty, inferred from HTTP method.
     */
    String action() default "";

    /**
     * The product context for evaluation (e.g., "LMS", "WFM").
     * If empty, will be detected from X-Product-Code header.
     */
    String product() default "";

    /**
     * Whether to skip policy evaluation (useful for public endpoints).
     */
    boolean skip() default false;
}

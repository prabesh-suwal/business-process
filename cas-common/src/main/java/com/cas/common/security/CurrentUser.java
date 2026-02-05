package com.cas.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the current UserContext into controller method
 * parameters.
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 * @GetMapping("/profile")
 * public UserProfile getProfile(@CurrentUser UserContext user) {
 *     return profileService.getProfile(user.getUserId());
 * }
 * }
 * </pre>
 * 
 * Requires CurrentUserArgumentResolver to be registered in your
 * WebMvcConfigurer.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {

    /**
     * Whether the user context is required. If true (default) and no user context
     * is available, an exception will be thrown. If false, null will be injected.
     */
    boolean required() default true;
}

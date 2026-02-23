package com.cas.common.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skip automatic {@link ApiResponse} wrapping for this endpoint.
 * Use on controller methods that:
 * <ul>
 * <li>Return pass-through proxy responses (already wrapped)</li>
 * <li>Must conform to external specs (OAuth2 token responses, OpenID
 * Connect)</li>
 * <li>Serve as webhook callbacks with fixed response shapes</li>
 * </ul>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RawResponse {
}

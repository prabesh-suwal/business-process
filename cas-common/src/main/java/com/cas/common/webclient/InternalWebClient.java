package com.cas.common.webclient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Qualifier annotation for the internal WebClient.Builder that automatically
 * propagates UserContext headers (X-User-Id, X-Roles, etc.) for
 * service-to-service calls.
 * 
 * Use this on any
 * {@link org.springframework.web.reactive.function.client.WebClient.Builder}
 * field that calls internal microservices (memo-service, workflow-service,
 * etc.).
 * 
 * For external service calls (CBS, WALLET, etc.), use the default (unqualified)
 * WebClient.Builder which does NOT propagate user context.
 * 
 * <pre>
 * &#64;InternalWebClient
 * private final WebClient.Builder webClientBuilder;
 * </pre>
 * 
 * <b>Important:</b> When using with Lombok's {@code @RequiredArgsConstructor},
 * add the following to your module's {@code lombok.config}:
 * 
 * <pre>
 * lombok.copyableAnnotations += com.cas.common.webclient.InternalWebClient
 * </pre>
 */
@Qualifier("internalWebClient")
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalWebClient {
}

package com.lms.gateway.config;

import com.lms.gateway.config.LmsGatewayProperties;
import com.lms.gateway.filter.CookiePropagationGatewayFilterFactory;
import com.lms.gateway.filter.JwtAuthenticationGatewayFilterFactory;
import com.lms.gateway.filter.ScopeEnforcementGatewayFilterFactory;
import com.lms.gateway.service.JwtValidationService;
import com.lms.gateway.service.ScopeEnforcementService;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

        private final String CAS_URL = "http://localhost:9000";
        private final String MEMO_URL = "http://localhost:9008";
        private final String LMS_URL = "http://localhost:9005";
        private final String PERSON_URL = "http://localhost:9007";
        private final String WORKFLOW_URL = "http://localhost:9002";
        private final String FORM_URL = "http://localhost:9006";
        private final String ORG_URL = "http://localhost:9003"; // Organization service

        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                        JwtValidationService jwtValidationService,
                        LmsGatewayProperties gatewayProperties,
                        ScopeEnforcementService scopeEnforcementService) {

                // Manually instantiate filters to avoid generic type erasure issues with
                // proxies
                var cookiePropagation = new CookiePropagationGatewayFilterFactory();
                var jwtAuthentication = new JwtAuthenticationGatewayFilterFactory(jwtValidationService,
                                gatewayProperties);
                var scopeEnforcement = new ScopeEnforcementGatewayFilterFactory(scopeEnforcementService);

                return builder.routes()
                                // --- Auth Routes ---
                                .route("auth-login", r -> r.path("/auth/login").and().method("POST")
                                                .filters(f -> f.filter(cookiePropagation.apply((Object c) -> {
                                                }))
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                .route("auth-session", r -> r.path("/auth/session").and().method("GET")
                                                .filters(f -> f.filter(cookiePropagation.apply((Object c) -> {
                                                }))
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                .route("auth-token-for-product", r -> r.path("/auth/token-for-product").and()
                                                .method("POST")
                                                .filters(f -> f.filter(cookiePropagation.apply((Object c) -> {
                                                }))
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                .route("auth-logout-global", r -> r.path("/auth/logout/global").and().method("POST")
                                                .filters(f -> f.filter(cookiePropagation.apply((Object c) -> {
                                                }))
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                .route("auth-refresh", r -> r.path("/auth/refresh").and().method("POST")
                                                .filters(f -> f.filter(cookiePropagation.apply((Object c) -> {
                                                }))
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                // --- MMS (Memo) Routes ---

                                .route("memo-service-memos", r -> r.path("/memos/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memos(?<segment>/?.*)",
                                                                                "/api/memos${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                .route("memo-service-config", r -> r.path("/memo-config/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo-config(?<segment>/?.*)",
                                                                                "/api/config${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                // Memo API - /memo/api/memos/** -> /api/memos/**
                                .route("memo-api-memos", r -> r.path("/memo/api/memos/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo/api/memos(?<segment>/?.*)",
                                                                                "/api/memos${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                // Memo Tasks - proxied through memo-service to workflow-service
                                .route("memo-tasks", r -> r.path("/memo/api/tasks/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo/api/tasks(?<segment>/?.*)",
                                                                                "/api/tasks${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                // Memo Topics & Gateway Config - /memo/api/topics/** -> /api/topics/**
                                .route("memo-topics", r -> r.path("/memo/api/topics/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo/api/topics(?<segment>/?.*)",
                                                                                "/api/topics${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                // CAS Workflow Config - admin dropdowns for roles/groups/departments
                                .route("cas-workflow-config", r -> r.path("/cas-admin/workflow-config/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/cas-admin/workflow-config(?<segment>/?.*)",
                                                                                "/admin/workflow-config${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                // Memo Workflow Config - step config, gateway rules per topic
                                .route("memo-workflow-config", r -> r.path("/memo-admin/topics/*/workflow-config/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo-admin/topics/(?<topicId>[^/]+)/workflow-config(?<segment>/?.*)",
                                                                                "/api/topics/${topicId}/workflow-config${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                // --- LMS Routes ---
                                .route("lms-loan-products", r -> r.path("/lms/api/loan-products/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .filter(scopeEnforcement.apply((
                                                                                ScopeEnforcementGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/lms(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(LMS_URL))

                                .route("lms-loan-applications", r -> r.path("/lms/api/loan-applications/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .filter(scopeEnforcement.apply((
                                                                                ScopeEnforcementGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/lms(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(LMS_URL))

                                .route("lms-persons", r -> r.path("/lms/api/persons/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/lms/api/persons(?<segment>/?.*)",
                                                                                "/api/persons${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(PERSON_URL))

                                // --- Workflow Routes ---
                                .route("workflow-tasks", r -> r.path("/workflow/api/tasks/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-processes", r -> r.path("/workflow/api/processes/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-process-templates", r -> r.path("/workflow/api/process-templates/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-variables", r -> r.path("/workflow/api/workflow-variables/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                // --- Form Service Routes ---
                                .route("mms-form-service", r -> r.path("/memo/api/forms/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo/api/forms(?<segment>/?.*)",
                                                                                "/api/forms${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(FORM_URL))

                                .route("lms-form-service", r -> r.path("/lms/api/forms/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/lms/api/forms(?<segment>/?.*)",
                                                                                "/api/forms${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(FORM_URL))

                                // --- Organization Service Routes ---
                                .route("org-branches", r -> r.path("/org/api/branches", "/org/api/branches/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/api/branches(?<segment>/?.*)",
                                                                                "/api/branches${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-departments", r -> r.path("/org/api/departments", "/org/api/departments/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/api/departments(?<segment>/?.*)",
                                                                                "/api/departments${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-groups", r -> r.path("/org/api/groups", "/org/api/groups/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/api/groups(?<segment>/?.*)",
                                                                                "/api/groups${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-geo", r -> r.path("/org/api/geo/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/api/geo(?<segment>/?.*)",
                                                                                "/api/geo${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .build();
        }
}

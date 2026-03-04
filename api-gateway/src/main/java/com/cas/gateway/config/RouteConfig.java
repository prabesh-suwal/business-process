package com.cas.gateway.config;

import com.cas.gateway.filter.CookiePropagationGatewayFilterFactory;
import com.cas.gateway.filter.JwtAuthenticationGatewayFilterFactory;
import com.cas.gateway.filter.MakerCheckerGatewayFilterFactory;
import com.cas.gateway.filter.ScopeEnforcementGatewayFilterFactory;
import com.cas.gateway.service.JwtValidationService;
import com.cas.gateway.service.ScopeEnforcementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Unified route configuration for the API Gateway.
 * Combines routes from the former admin-gateway and gateway-product into a
 * single gateway.
 *
 * Route sections:
 * 1. Auth Routes (cookie-based, no JWT)
 * 2. JWKS (public)
 * 3. CAS Admin Routes (JWT-authenticated)
 * 4. Policy Engine Routes (JWT-authenticated)
 * 5. MMS (Memo) Routes (JWT-authenticated)
 * 6. LMS Routes (JWT + scope enforcement)
 * 7. Workflow Routes (JWT-authenticated)
 * 8. Form Routes (JWT-authenticated)
 * 9. Organization Routes (JWT-authenticated)
 * 10. Document Routes (JWT-authenticated)
 * 11. Audit Routes (JWT-authenticated)
 */
@Configuration
public class RouteConfig {

        @Value("${gateway.routes.cas-url:http://localhost:9000}")
        private String CAS_URL;

        @Value("${gateway.routes.memo-url:http://localhost:9008}")
        private String MEMO_URL;

        @Value("${gateway.routes.lms-url:http://localhost:9005}")
        private String LMS_URL;

        @Value("${gateway.routes.person-url:http://localhost:9007}")
        private String PERSON_URL;

        @Value("${gateway.routes.workflow-url:http://localhost:9002}")
        private String WORKFLOW_URL;

        @Value("${gateway.routes.form-url:http://localhost:9006}")
        private String FORM_URL;

        @Value("${gateway.routes.org-url:http://localhost:9003}")
        private String ORG_URL;

        @Value("${gateway.routes.document-url:http://localhost:9010}")
        private String DOCUMENT_URL;

        @Value("${gateway.routes.policy-url:http://localhost:9001}")
        private String POLICY_URL;

        @Value("${gateway.routes.audit-url:http://localhost:9009}")
        private String AUDIT_URL;

        @Value("${gateway.routes.maker-checker-url:http://localhost:9011}")
        private String MAKER_CHECKER_URL;

        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                        JwtValidationService jwtValidationService,
                        CasGatewayProperties gatewayProperties,
                        ScopeEnforcementService scopeEnforcementService,
                        MakerCheckerGatewayFilterFactory makerCheckerFilterFactory) {

                var cookiePropagation = new CookiePropagationGatewayFilterFactory();
                var jwtAuthentication = new JwtAuthenticationGatewayFilterFactory(jwtValidationService,
                                gatewayProperties);
                var scopeEnforcement = new ScopeEnforcementGatewayFilterFactory(scopeEnforcementService);
                var makerChecker = makerCheckerFilterFactory.apply(
                                (MakerCheckerGatewayFilterFactory.Config c) -> {
                                });

                return builder.routes()

                                // ==========================================================
                                // 1. AUTH ROUTES — Cookie-based, no JWT (SSO flow)
                                // ==========================================================

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

                                // Effective access context — JWT-authenticated
                                .route("auth-me", r -> r.path("/auth/api/me", "/auth/api/me/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/auth/api/me(?<segment>/?.*)",
                                                                                "/api/me${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                // ==========================================================
                                // 2. JWKS — Public endpoint
                                // ==========================================================

                                .route("jwks", r -> r.path("/.well-known/**")
                                                .uri(CAS_URL))

                                // ==========================================================
                                // 3. CAS ADMIN ROUTES — JWT-authenticated (admin-ui)
                                // ==========================================================

                                .route("cas-admin", r -> r.path("/admin/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .filter(makerChecker)
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                .route("cas-workflow-config", r -> r.path("/cas-admin/workflow-config/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/cas-admin/workflow-config(?<segment>/?.*)",
                                                                                "/admin/workflow-config${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(CAS_URL))

                                // ==========================================================
                                // 4. POLICY ENGINE ROUTES — JWT-authenticated (admin-ui)
                                // ==========================================================

                                .route("policy-engine", r -> r.path("/policies", "/policies/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/policies(?<segment>/?.*)",
                                                                                "/api/policies${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(POLICY_URL))

                                .route("policy-evaluate", r -> r.path("/evaluate/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/evaluate/(?<segment>.*)",
                                                                                "/api/evaluate/${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(POLICY_URL))

                                // ==========================================================
                                // 5. MMS (MEMO) ROUTES — JWT-authenticated (memo-ui)
                                // ==========================================================

                                .route("memo-service-memos", r -> r.path("/memos/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .filter(makerChecker)
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

                                .route("memo-api-memos", r -> r.path("/memo/api/memos/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo/api/memos(?<segment>/?.*)",
                                                                                "/api/memos${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                .route("memo-tasks", r -> r.path("/memo/api/tasks/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo/api/tasks(?<segment>/?.*)",
                                                                                "/api/tasks${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                .route("memo-topics", r -> r.path("/memo/api/topics/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo/api/topics(?<segment>/?.*)",
                                                                                "/api/topics${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                .route("memo-workflow-config", r -> r.path("/memo-admin/topics/*/workflow-config/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/memo-admin/topics/(?<topicId>[^/]+)/workflow-config(?<segment>/?.*)",
                                                                                "/api/topics/${topicId}/workflow-config${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MEMO_URL))

                                // ==========================================================
                                // 6. LMS ROUTES — JWT + Scope enforcement (lms-ui)
                                // ==========================================================

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

                                // ==========================================================
                                // 7. WORKFLOW ROUTES — JWT-authenticated
                                // ==========================================================

                                // Product-style routes: /workflow/api/...
                                .route("workflow-api-tasks", r -> r.path("/workflow/api/tasks/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-api-processes", r -> r.path("/workflow/api/processes/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-api-process-templates", r -> r
                                                .path("/workflow/api/process-templates/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-api-variables", r -> r.path("/workflow/api/workflow-variables/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-api-dmn", r -> r.path("/workflow/api/dmn", "/workflow/api/dmn/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow(?<segment>/?.*)", "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                // Admin-style routes: /workflow/templates/..., /workflow/instances/...
                                .route("workflow-templates", r -> r
                                                .path("/workflow/templates", "/workflow/templates/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow/templates(?<segment>/?.*)",
                                                                                "/api/process-templates${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-instances", r -> r
                                                .path("/workflow/instances", "/workflow/instances/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow/instances(?<segment>/?.*)",
                                                                                "/api/process-instances${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-tasks-admin", r -> r.path("/workflow/tasks", "/workflow/tasks/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow/tasks(?<segment>/?.*)",
                                                                                "/api/tasks${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-history", r -> r.path("/workflow/history", "/workflow/history/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow/history(?<segment>/?.*)",
                                                                                "/api/history${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-configs", r -> r.path("/workflow/workflow-configs",
                                                "/workflow/workflow-configs/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow/workflow-configs(?<segment>/?.*)",
                                                                                "/api/workflow-configs${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                .route("workflow-dmn-admin", r -> r.path("/workflow/dmn", "/workflow/dmn/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/workflow/dmn(?<segment>/?.*)",
                                                                                "/api/dmn${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(WORKFLOW_URL))

                                // ==========================================================
                                // 8. FORM ROUTES — JWT-authenticated
                                // ==========================================================

                                // Product-style routes
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

                                // Admin-style routes
                                .route("form-definitions", r -> r.path("/forms/definitions", "/forms/definitions/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/forms/definitions(?<segment>/?.*)",
                                                                                "/api/forms${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(FORM_URL))

                                .route("form-submissions", r -> r.path("/forms/submissions", "/forms/submissions/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/forms/submissions(?<segment>/?.*)",
                                                                                "/api/submissions${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(FORM_URL))

                                // ==========================================================
                                // 9. ORGANIZATION ROUTES — JWT-authenticated
                                // ==========================================================

                                // Product-style routes: /org/api/...
                                .route("org-api-branches", r -> r.path("/org/api/branches", "/org/api/branches/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/api/branches(?<segment>/?.*)",
                                                                                "/api/branches${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-api-departments", r -> r
                                                .path("/org/api/departments", "/org/api/departments/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/api/departments(?<segment>/?.*)",
                                                                                "/api/departments${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-api-groups", r -> r.path("/org/api/groups", "/org/api/groups/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/api/groups(?<segment>/?.*)",
                                                                                "/api/groups${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-api-geo", r -> r.path("/org/api/geo/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/api/geo(?<segment>/?.*)",
                                                                                "/api/geo${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                // Admin-style routes: /org/branches/..., /org/geo/...
                                .route("org-branches-admin", r -> r.path("/org/branches", "/org/branches/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/branches(?<segment>/?.*)",
                                                                                "/api/branches${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-departments-admin", r -> r.path("/org/departments", "/org/departments/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/departments(?<segment>/?.*)",
                                                                                "/api/departments${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-groups-admin", r -> r.path("/org/groups", "/org/groups/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/groups(?<segment>/?.*)",
                                                                                "/api/groups${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                .route("org-geo-admin", r -> r.path("/org/geo/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/org/geo/(?<segment>.*)",
                                                                                "/api/geo/${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(ORG_URL))

                                // ==========================================================
                                // 10. DOCUMENT ROUTES — JWT-authenticated
                                // ==========================================================

                                .route("document-service", r -> r.path("/documents/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/documents(?<segment>/?.*)",
                                                                                "/api/documents${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(DOCUMENT_URL))

                                // ==========================================================
                                // 11. AUDIT ROUTES — JWT-authenticated (admin-ui)
                                // ==========================================================

                                .route("audit-events", r -> r.path("/audit", "/audit/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/audit(?<segment>/?.*)",
                                                                                "/api/audit-events${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(AUDIT_URL))

                                // ==========================================================
                                // 12. MAKER-CHECKER ROUTES — JWT-authenticated
                                // ==========================================================

                                .route("maker-checker-api", r -> r.path("/maker-checker/api/**")
                                                .filters(f -> f.filter(jwtAuthentication.apply(
                                                                (JwtAuthenticationGatewayFilterFactory.Config c) -> {
                                                                }))
                                                                .rewritePath("/maker-checker(?<segment>/?.*)",
                                                                                "${segment}")
                                                                .removeResponseHeader("WWW-Authenticate"))
                                                .uri(MAKER_CHECKER_URL))

                                .build();
        }
}

package com.cas.admin.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Aggregates OpenAPI specifications from all downstream services reachable
 * through admin-gateway into a single merged spec.
 *
 * Each service is fetched ONCE, and its paths are rewritten to match the
 * gateway's routing configuration defined in application.yml.
 */
@RestController
public class AdminOpenApiAggregatorController {

    private static final Logger log = LoggerFactory.getLogger(AdminOpenApiAggregatorController.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Service URLs from admin-gateway application.yml routes
    private static final String CAS_URL = "http://localhost:9000";
    private static final String POLICY_URL = "http://localhost:9001";
    private static final String WORKFLOW_URL = "http://localhost:9002";
    private static final String ORG_URL = "http://localhost:9003";
    private static final String FORM_URL = "http://localhost:9006";
    private static final String AUDIT_URL = "http://localhost:9009";

    public AdminOpenApiAggregatorController() {
        this.webClient = WebClient.create();
        this.objectMapper = new ObjectMapper();
    }

    private record ServiceSpec(String name, String baseUrl, LinkedHashMap<String, String> pathRewrites) {
    }

    @GetMapping(value = "/v3/api-docs/merged", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> getMergedOpenApi() {
        List<ServiceSpec> services = buildServiceSpecs();

        return Flux.fromIterable(services)
                .flatMap(spec -> webClient.get()
                        .uri(spec.baseUrl() + "/v3/api-docs")
                        .retrieve()
                        .bodyToMono(String.class)
                        .map(json -> {
                            try {
                                return objectMapper.readTree(json);
                            } catch (Exception e) {
                                log.error("Failed to parse OpenAPI JSON from {}", spec.name(), e);
                                return (JsonNode) objectMapper.createObjectNode();
                            }
                        })
                        .map(rawSpec -> rewritePaths(rawSpec, spec))
                        .doOnNext(rewritten -> log.debug("Fetched {} paths from {}",
                                rewritten.has("paths") ? rewritten.get("paths").size() : 0, spec.name()))
                        .onErrorResume(e -> {
                            log.warn("Failed to fetch OpenAPI from {} ({}): {}", spec.name(), spec.baseUrl(),
                                    e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .map(this::mergeOpenApiSpecs)
                .map(node -> {
                    try {
                        return objectMapper.writeValueAsString(node);
                    } catch (Exception e) {
                        log.error("Failed to serialize merged OpenAPI", e);
                        return "{}";
                    }
                });
    }

    /**
     * Build the list of downstream services and their path rewrite rules.
     * These MUST match the route definitions in admin-gateway's application.yml.
     */
    private List<ServiceSpec> buildServiceSpecs() {
        List<ServiceSpec> services = new ArrayList<>();

        // CAS Server (Auth + Admin)
        services.add(new ServiceSpec("cas-server", CAS_URL, orderedMap(
                "/auth", "/auth",
                "/admin", "/admin")));

        // Policy Engine
        services.add(new ServiceSpec("policy-engine", POLICY_URL, orderedMap(
                "/api/policies", "/policies",
                "/api/evaluate", "/evaluate")));

        // Organization Service
        services.add(new ServiceSpec("org-service", ORG_URL, orderedMap(
                "/api/branches", "/org/branches",
                "/api/departments", "/org/departments",
                "/api/groups", "/org/groups",
                "/api/geo", "/org/geo")));

        // Workflow Service
        services.add(new ServiceSpec("workflow-service", WORKFLOW_URL, orderedMap(
                "/api/process-templates", "/workflow/templates",
                "/api/process-instances", "/workflow/instances",
                "/api/workflow-configs", "/workflow/workflow-configs",
                "/api/workflow-variables", "/workflow/workflow-variables",
                "/api/tasks", "/workflow/tasks",
                "/api/history", "/workflow/history",
                "/api/dmn", "/workflow/dmn")));

        // Form Service
        services.add(new ServiceSpec("form-service", FORM_URL, orderedMap(
                "/api/forms", "/forms/definitions",
                "/api/submissions", "/forms/submissions")));

        // Audit Service
        services.add(new ServiceSpec("audit-service", AUDIT_URL, orderedMap(
                "/api/audit-events", "/audit")));

        return services;
    }

    private JsonNode rewritePaths(JsonNode spec, ServiceSpec serviceSpec) {
        if (!spec.has("paths") || !spec.get("paths").isObject()) {
            log.warn("No paths found in OpenAPI spec from {}", serviceSpec.name());
            return spec;
        }

        ObjectNode originalPaths = (ObjectNode) spec.get("paths");
        ObjectNode rewrittenPaths = objectMapper.createObjectNode();

        Iterator<Map.Entry<String, JsonNode>> fields = originalPaths.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String originalPath = entry.getKey();

            // Find the longest matching prefix
            String bestMatch = null;
            for (String prefix : serviceSpec.pathRewrites().keySet()) {
                if (originalPath.startsWith(prefix)) {
                    if (bestMatch == null || prefix.length() > bestMatch.length()) {
                        bestMatch = prefix;
                    }
                }
            }

            if (bestMatch != null) {
                String newPrefix = serviceSpec.pathRewrites().get(bestMatch);
                String newPath = newPrefix + originalPath.substring(bestMatch.length());
                rewrittenPaths.set(newPath, entry.getValue());
            }
        }

        if (rewrittenPaths.isEmpty()) {
            log.warn("All paths filtered out for {} (had {} original paths)", serviceSpec.name(), originalPaths.size());
            return objectMapper.createObjectNode();
        }

        ObjectNode newSpec = spec.deepCopy();
        newSpec.set("paths", rewrittenPaths);
        return newSpec;
    }

    private JsonNode mergeOpenApiSpecs(List<JsonNode> specs) {
        ObjectNode merged = objectMapper.createObjectNode();
        merged.put("openapi", "3.0.1");

        ObjectNode info = objectMapper.createObjectNode();
        info.put("title", "Admin API (Aggregated)");
        info.put("description", "Unified API documentation for the Admin gateway. " +
                "All endpoints are routed through admin-gateway (port 8085).");
        info.put("version", "1.0");
        merged.set("info", info);

        ObjectNode mergedPaths = objectMapper.createObjectNode();
        ObjectNode mergedSchemas = objectMapper.createObjectNode();
        ObjectNode mergedSecuritySchemes = objectMapper.createObjectNode();
        ArrayNode mergedTags = objectMapper.createArrayNode();
        ArrayNode mergedSecurity = objectMapper.createArrayNode();

        for (JsonNode spec : specs) {
            if (spec == null || spec.isEmpty())
                continue;

            JsonNode paths = spec.get("paths");
            if (paths != null && paths.isObject()) {
                paths.fields().forEachRemaining(e -> mergedPaths.set(e.getKey(), e.getValue()));
            }

            JsonNode components = spec.get("components");
            if (components != null && components.isObject()) {
                JsonNode schemas = components.get("schemas");
                if (schemas != null && schemas.isObject()) {
                    schemas.fields().forEachRemaining(e -> mergedSchemas.set(e.getKey(), e.getValue()));
                }
                JsonNode secSchemes = components.get("securitySchemes");
                if (secSchemes != null && secSchemes.isObject()) {
                    secSchemes.fields().forEachRemaining(e -> mergedSecuritySchemes.set(e.getKey(), e.getValue()));
                }
            }

            JsonNode tags = spec.get("tags");
            if (tags != null && tags.isArray()) {
                Set<String> existingTagNames = new HashSet<>();
                for (JsonNode t : mergedTags) {
                    if (t.has("name"))
                        existingTagNames.add(t.get("name").asText());
                }
                for (JsonNode tag : tags) {
                    if (tag.has("name") && !existingTagNames.contains(tag.get("name").asText())) {
                        mergedTags.add(tag);
                        existingTagNames.add(tag.get("name").asText());
                    }
                }
            }

            JsonNode security = spec.get("security");
            if (security != null && security.isArray() && mergedSecurity.isEmpty()) {
                for (JsonNode sec : security) {
                    mergedSecurity.add(sec);
                }
            }
        }

        merged.set("paths", mergedPaths);

        ObjectNode components = objectMapper.createObjectNode();
        components.set("schemas", mergedSchemas);
        components.set("securitySchemes", mergedSecuritySchemes);
        merged.set("components", components);

        merged.set("tags", mergedTags);
        merged.set("security", mergedSecurity);

        log.info("Merged OpenAPI: {} paths, {} schemas, {} tags",
                mergedPaths.size(), mergedSchemas.size(), mergedTags.size());

        return merged;
    }

    @SafeVarargs
    private static LinkedHashMap<String, String> orderedMap(String... keyValues) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}

package com.lms.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Aggregates OpenAPI specifications from all downstream services reachable
 * through gateway-product (LMS gateway) into a single merged spec.
 *
 * Each service is fetched ONCE, and its paths are rewritten to match the
 * gateway's routing configuration in RouteConfig.java.
 */
@RestController
public class LmsOpenApiAggregatorController {

    private static final Logger log = LoggerFactory.getLogger(LmsOpenApiAggregatorController.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gateway.routes.cas-url:http://localhost:9000}")
    private String casUrl;

    @Value("${gateway.routes.memo-url:http://localhost:9008}")
    private String memoUrl;

    @Value("${gateway.routes.lms-url:http://localhost:9005}")
    private String lmsUrl;

    @Value("${gateway.routes.person-url:http://localhost:9007}")
    private String personUrl;

    @Value("${gateway.routes.workflow-url:http://localhost:9002}")
    private String workflowUrl;

    @Value("${gateway.routes.form-url:http://localhost:9006}")
    private String formUrl;

    @Value("${gateway.routes.org-url:http://localhost:9003}")
    private String orgUrl;

    @Value("${gateway.routes.document-url:http://localhost:9010}")
    private String documentUrl;

    public LmsOpenApiAggregatorController() {
        this.webClient = WebClient.create();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Defines a downstream service: its name, base URL, and how paths should
     * be rewritten to match the gateway's routing.
     *
     * @param name         Human-readable service name
     * @param baseUrl      Base URL of the service (e.g., http://localhost:9008)
     * @param pathRewrites Map of original path prefix → gateway path prefix.
     *                     Order matters: longer prefixes should be checked first.
     */
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
     * Each entry maps: downstream service path prefix → gateway-exposed path.
     * These MUST match the rewrite rules in RouteConfig.java.
     */
    private List<ServiceSpec> buildServiceSpecs() {
        List<ServiceSpec> services = new ArrayList<>();

        // CAS Server (Auth endpoints)
        services.add(new ServiceSpec("cas-server", casUrl, orderedMap(
                "/auth", "/auth",
                "/admin/workflow-config", "/cas-admin/workflow-config")));

        // Memo Service
        services.add(new ServiceSpec("memo-service", memoUrl, orderedMap(
                "/api/memos", "/memo/api/memos",
                "/api/tasks", "/memo/api/tasks",
                "/api/topics", "/memo/api/topics",
                "/api/config", "/memo-config")));

        // LMS Service
        services.add(new ServiceSpec("lms-service", lmsUrl, orderedMap(
                "/api/loan-products", "/lms/api/loan-products",
                "/api/loan-applications", "/lms/api/loan-applications")));

        // Person Service
        services.add(new ServiceSpec("person-service", personUrl, orderedMap(
                "/api/persons", "/lms/api/persons")));

        // Workflow Service
        services.add(new ServiceSpec("workflow-service", workflowUrl, orderedMap(
                "/api/process-templates", "/workflow/api/process-templates",
                "/api/workflow-variables", "/workflow/api/workflow-variables",
                "/api/processes", "/workflow/api/processes",
                "/api/tasks", "/workflow/api/tasks",
                "/api/dmn", "/workflow/api/dmn")));

        // Form Service
        services.add(new ServiceSpec("form-service", formUrl, orderedMap(
                "/api/forms", "/memo/api/forms")));

        // Organization Service
        services.add(new ServiceSpec("org-service", orgUrl, orderedMap(
                "/api/branches", "/org/api/branches",
                "/api/departments", "/org/api/departments",
                "/api/groups", "/org/api/groups",
                "/api/geo", "/org/api/geo")));

        // Document Service
        services.add(new ServiceSpec("document-service", documentUrl, orderedMap(
                "/api/documents", "/documents")));

        return services;
    }

    /**
     * Rewrite all paths in a service's OpenAPI spec to match the gateway routes.
     * For each path in the spec, find the longest matching prefix from the
     * service's pathRewrites map and apply the substitution.
     */
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
            // Paths that don't match any rewrite rule are silently skipped
        }

        if (rewrittenPaths.isEmpty()) {
            log.warn("All paths filtered out for {} (had {} original paths)", serviceSpec.name(), originalPaths.size());
            return objectMapper.createObjectNode();
        }

        ObjectNode newSpec = spec.deepCopy();
        newSpec.set("paths", rewrittenPaths);
        return newSpec;
    }

    /**
     * Merge multiple OpenAPI specs into a single unified document.
     */
    private JsonNode mergeOpenApiSpecs(List<JsonNode> specs) {
        ObjectNode merged = objectMapper.createObjectNode();
        merged.put("openapi", "3.0.1");

        ObjectNode info = objectMapper.createObjectNode();
        info.put("title", "LMS & MMS API (Aggregated)");
        info.put("description", "Unified API documentation for the LMS product gateway. " +
                "All endpoints are routed through gateway-product (port 8086).");
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

            // Merge paths
            JsonNode paths = spec.get("paths");
            if (paths != null && paths.isObject()) {
                paths.fields().forEachRemaining(e -> mergedPaths.set(e.getKey(), e.getValue()));
            }

            // Merge components
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

            // Merge tags (deduplicate by name)
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

            // Take security from the first spec that has it
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

    /**
     * Helper to create a LinkedHashMap preserving insertion order (longest prefixes
     * first).
     */
    @SafeVarargs
    private static LinkedHashMap<String, String> orderedMap(String... keyValues) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}

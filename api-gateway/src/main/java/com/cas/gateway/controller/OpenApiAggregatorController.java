package com.cas.gateway.controller;

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
 * Aggregates OpenAPI specifications from all downstream services
 * into a single merged spec for the unified API Gateway.
 */
@RestController
public class OpenApiAggregatorController {

    private static final Logger log = LoggerFactory.getLogger(OpenApiAggregatorController.class);
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

    @Value("${gateway.routes.policy-url:http://localhost:9001}")
    private String policyUrl;

    @Value("${gateway.routes.audit-url:http://localhost:9009}")
    private String auditUrl;

    public OpenApiAggregatorController() {
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

    private List<ServiceSpec> buildServiceSpecs() {
        List<ServiceSpec> services = new ArrayList<>();

        services.add(new ServiceSpec("cas-server", casUrl, orderedMap(
                "/auth", "/auth",
                "/admin/workflow-config", "/cas-admin/workflow-config",
                "/admin", "/admin")));

        services.add(new ServiceSpec("policy-engine", policyUrl, orderedMap(
                "/api/policies", "/policies",
                "/api/evaluate", "/evaluate")));

        services.add(new ServiceSpec("memo-service", memoUrl, orderedMap(
                "/api/memos", "/memo/api/memos",
                "/api/tasks", "/memo/api/tasks",
                "/api/topics", "/memo/api/topics",
                "/api/config", "/memo-config")));

        services.add(new ServiceSpec("lms-service", lmsUrl, orderedMap(
                "/api/loan-products", "/lms/api/loan-products",
                "/api/loan-applications", "/lms/api/loan-applications")));

        services.add(new ServiceSpec("person-service", personUrl, orderedMap(
                "/api/persons", "/lms/api/persons")));

        services.add(new ServiceSpec("workflow-service", workflowUrl, orderedMap(
                "/api/process-templates", "/workflow/api/process-templates",
                "/api/workflow-variables", "/workflow/api/workflow-variables",
                "/api/processes", "/workflow/api/processes",
                "/api/tasks", "/workflow/api/tasks",
                "/api/dmn", "/workflow/api/dmn")));

        services.add(new ServiceSpec("form-service", formUrl, orderedMap(
                "/api/forms", "/memo/api/forms")));

        services.add(new ServiceSpec("org-service", orgUrl, orderedMap(
                "/api/branches", "/org/api/branches",
                "/api/departments", "/org/api/departments",
                "/api/groups", "/org/api/groups",
                "/api/geo", "/org/api/geo")));

        services.add(new ServiceSpec("document-service", documentUrl, orderedMap(
                "/api/documents", "/documents")));

        services.add(new ServiceSpec("audit-service", auditUrl, orderedMap(
                "/api/audit-events", "/audit")));

        return services;
    }

    private JsonNode rewritePaths(JsonNode spec, ServiceSpec serviceSpec) {
        if (!spec.has("paths") || !spec.get("paths").isObject()) {
            return spec;
        }

        ObjectNode originalPaths = (ObjectNode) spec.get("paths");
        ObjectNode rewrittenPaths = objectMapper.createObjectNode();

        Iterator<Map.Entry<String, JsonNode>> fields = originalPaths.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String originalPath = entry.getKey();

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
        info.put("title", "Unified API Gateway (Aggregated)");
        info.put("description",
                "Unified API documentation for all services routed through the API Gateway (port 8085).");
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

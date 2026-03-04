package com.cas.gateway.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScopeEnforcementService {

    @Value("classpath:scope-mappings.yml")
    private Resource scopeMappingsResource;

    private final List<ScopeRule> rules = new ArrayList<>();

    @PostConstruct
    public void init() {
        try (InputStream is = scopeMappingsResource.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(is);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mappings = (List<Map<String, Object>>) config.get("mappings");

            if (mappings != null) {
                for (Map<String, Object> mapping : mappings) {
                    String pattern = (String) mapping.get("pattern");
                    @SuppressWarnings("unchecked")
                    List<String> scopes = (List<String>) mapping.get("scopes");

                    if (pattern != null && scopes != null) {
                        rules.add(new ScopeRule(pattern, new HashSet<>(scopes)));
                    }
                }
            }

            log.info("Loaded {} scope mapping rules", rules.size());
        } catch (Exception e) {
            log.error("Failed to load scope mappings", e);
        }
    }

    public Set<String> getRequiredScopes(String method, String path) {
        String requestPattern = method + " " + path;

        for (ScopeRule rule : rules) {
            if (rule.matches(requestPattern)) {
                log.debug("Matched rule: {} -> {}", rule.pattern, rule.requiredScopes);
                return rule.requiredScopes;
            }
        }

        log.debug("No scope rule matched for: {}", requestPattern);
        return Collections.emptySet();
    }

    public boolean hasRequiredScopes(Set<String> userScopes, Set<String> requiredScopes) {
        if (requiredScopes.isEmpty()) {
            return true;
        }

        if (userScopes.contains("*")) {
            return true;
        }

        for (String required : requiredScopes) {
            if (!hasMatchingScope(userScopes, required)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasMatchingScope(Set<String> userScopes, String requiredScope) {
        if (userScopes.contains(requiredScope)) {
            return true;
        }

        for (String userScope : userScopes) {
            if (userScope.endsWith(":*")) {
                String prefix = userScope.substring(0, userScope.length() - 1);
                if (requiredScope.startsWith(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static class ScopeRule {
        final String pattern;
        final Pattern regex;
        final Set<String> requiredScopes;

        ScopeRule(String pattern, Set<String> requiredScopes) {
            this.pattern = pattern;
            this.requiredScopes = requiredScopes;

            String regexPattern = pattern
                    .replace("/**", "/.*")
                    .replace("/*", "/[^/]+")
                    .replace("/", "\\/");

            this.regex = Pattern.compile(regexPattern);
        }

        boolean matches(String request) {
            return regex.matcher(request).matches();
        }
    }
}

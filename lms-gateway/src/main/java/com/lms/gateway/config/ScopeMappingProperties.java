package com.lms.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class ScopeMappingProperties {

    private List<ScopeMapping> mappings = new ArrayList<>();

    @Data
    public static class ScopeMapping {
        private String pattern;
        private List<String> scopes = new ArrayList<>();
    }
}

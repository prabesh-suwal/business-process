package com.cas.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class CasGatewayProperties {

    private Set<String> productCodes = Set.of("LMS", "MMS", "ADMIN", "CAS_ADMIN");

    private JwtProperties jwt = new JwtProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();

    public Set<String> getAudiences() {
        return productCodes.stream()
                .map(code -> code.toLowerCase() + "-api")
                .collect(Collectors.toSet());
    }

    @Data
    public static class JwtProperties {
        private String jwksUri = "http://localhost:9000/.well-known/jwks.json";
        private String issuer = "http://localhost:9000";
    }

    @Data
    public static class RateLimitProperties {
        private boolean enabled = true;
        private int defaultLimit = 100;
        private int windowSeconds = 60;
    }
}

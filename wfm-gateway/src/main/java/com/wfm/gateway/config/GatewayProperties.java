package com.wfm.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private String productCode = "LMS";
    private String audience = "lms-api";

    private JwtProperties jwt = new JwtProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();

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

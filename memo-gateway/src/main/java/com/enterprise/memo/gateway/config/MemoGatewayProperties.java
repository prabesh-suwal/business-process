package com.enterprise.memo.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway")
@Getter
@Setter
public class MemoGatewayProperties {
    private String productCode;
    private String audience;
    private JwtProperties jwt;
    private RateLimitProperties rateLimit;

    @Getter
    @Setter
    public static class JwtProperties {
        private String jwksUri;
        private String issuer;
    }

    @Getter
    @Setter
    public static class RateLimitProperties {
        private boolean enabled;
        private int defaultLimit;
        private int windowSeconds;
    }
}

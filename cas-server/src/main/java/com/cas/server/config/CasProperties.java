package com.cas.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "cas")
public class CasProperties {

    private String issuer = "http://localhost:9000";

    private JwtProperties jwt = new JwtProperties();
    private SessionProperties session = new SessionProperties();
    private SecurityProperties security = new SecurityProperties();

    @Data
    public static class JwtProperties {
        private Duration accessTokenTtl = Duration.ofMinutes(5);
        private Duration refreshTokenTtl = Duration.ofDays(30);
        private String keyId = "cas-key-1";
    }

    @Data
    public static class SessionProperties {
        private String cookieName = "CAS_SESSION";
        private String cookieDomain = "localhost";
        private boolean cookieSecure = false;
        private boolean cookieHttpOnly = true;
        private Duration timeout = Duration.ofHours(8);
    }

    @Data
    public static class SecurityProperties {
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:5173");
    }
}

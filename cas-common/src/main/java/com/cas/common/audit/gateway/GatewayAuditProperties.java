package com.cas.common.audit.gateway;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for gateway audit logging.
 */
@Data
@ConfigurationProperties(prefix = "audit.gateway")
public class GatewayAuditProperties {

        /**
         * Whether to log request body in audit events.
         */
        private boolean logRequestBody = true;

        /**
         * Whether to log response body in audit events.
         */
        private boolean logResponseBody = true;

        /**
         * Maximum body size to log (bytes). Bodies larger than this will be truncated.
         */
        private int maxBodySize = 4096;

        /**
         * Whether to log error response bodies even if logResponseBody is false.
         * This enables error detail logging for 4xx/5xx responses.
         */
        private boolean logErrorDetails = true;

        /**
         * Fields to mask in request/response bodies for security.
         */
        private List<String> sensitiveFields = new ArrayList<>(List.of(
                        "password",
                        "secret",
                        "token",
                        "apiKey",
                        "api_key",
                        "clientSecret",
                        "client_secret",
                        "accessToken",
                        "access_token",
                        "refreshToken",
                        "refresh_token",
                        "credential",
                        "credentials",
                        "authorization",
                        "privateKey",
                        "private_key"));

        /**
         * URL patterns to exclude from body logging (e.g., file uploads).
         */
        private List<String> excludeBodyPatterns = new ArrayList<>(List.of(
                        "/upload",
                        "/files",
                        "/documents"));
}

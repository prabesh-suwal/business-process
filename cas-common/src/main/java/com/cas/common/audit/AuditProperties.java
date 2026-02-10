package com.cas.common.audit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for audit logging.
 */
@Data
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    /**
     * Whether audit logging is enabled.
     */
    private boolean enabled = true;

    /**
     * URL of the audit service.
     */
    private String serviceUrl = "http://localhost:9009";

    /**
     * Timeout in milliseconds for audit service calls.
     */
    private int timeoutMs = 5000;

    /**
     * Whether to fail silently if audit service is unavailable.
     */
    private boolean failSilently = true;

    /**
     * Service name to use in audit events.
     */
    private String serviceName;

    /**
     * Default product code to use if not specified.
     */
    private String defaultProductCode;
}

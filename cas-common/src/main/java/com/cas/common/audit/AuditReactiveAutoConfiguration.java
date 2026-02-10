package com.cas.common.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;

/**
 * Reactive-specific audit auto-configuration.
 * 
 * Only activates in reactive web applications (e.g., gateway-product,
 * admin-gateway).
 * 
 * Currently a placeholder — gateways configure their own filters manually
 * via their respective AuditConfig classes (which create GatewayAuditFilter
 * and GatewayApiLogFilter beans).
 * 
 * Future enhancements:
 * - Reactive correlation ID propagation via Reactor Context
 * - Reactive audit event publisher (non-blocking)
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditReactiveAutoConfiguration {
    // Gateway-specific filters are configured manually in each gateway's
    // AuditConfig
    // This class serves as the extension point for future reactive audit
    // infrastructure
}

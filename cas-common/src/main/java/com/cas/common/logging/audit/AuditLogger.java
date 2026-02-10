package com.cas.common.logging.audit;

/**
 * Entry point for logging audit events.
 * Inject this into your services to log compliance/audit events.
 * 
 * Usage:
 * 
 * <pre>
 * @Autowired
 * private AuditLogger auditLogger;
 * 
 * public void approveLoan(UUID loanId) {
 *     Loan oldLoan = repo.findById(loanId);
 *     // ... business logic ...
 * 
 *     auditLogger.log()
 *             .eventType(AuditEventType.APPROVE)
 *             .module("LOAN")
 *             .entity("Loan", loanId)
 *             .oldValue(oldLoan)
 *             .newValue(newLoan)
 *             .success();
 * }
 * </pre>
 * 
 * This bean is registered by
 * {@link com.cas.common.logging.LoggingAutoConfiguration}.
 * Do NOT add @Component — it is managed by explicit @Bean definition.
 */
public class AuditLogger {

    private final AuditLogPublisher publisher;
    private final String serviceName;

    public AuditLogger(AuditLogPublisher publisher, String serviceName) {
        this.publisher = publisher;
        this.serviceName = serviceName;
    }

    /**
     * Start building a new audit log entry.
     * 
     * @return A new builder for fluent configuration
     */
    public AuditLogBuilder log() {
        return new AuditLogBuilder(publisher, serviceName);
    }
}

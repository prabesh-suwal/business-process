package com.cas.common.logging.activity;

/**
 * Entry point for logging activity events.
 * Inject this into your services to log user activities for timeline.
 * 
 * Usage:
 * 
 * <pre>
 * @Autowired
 * private ActivityLogger activityLogger;
 * 
 * public Report downloadReport(UUID reportId) {
 *     Report report = repo.findById(reportId);
 * 
 *     activityLogger.log()
 *             .type(ActivityType.DOWNLOAD)
 *             .module("REPORTS")
 *             .entity("Report", reportId)
 *             .description("Downloaded " + report.getName())
 *             .success();
 * 
 *     return report;
 * }
 * </pre>
 * 
 * This bean is registered by
 * {@link com.cas.common.logging.LoggingAutoConfiguration}.
 * Do NOT add @Component — it is managed by explicit @Bean definition.
 */
public class ActivityLogger {

    private final ActivityLogPublisher publisher;

    public ActivityLogger(ActivityLogPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Start building a new activity log entry.
     * 
     * @return A new builder for fluent configuration
     */
    public ActivityLogBuilder log() {
        return new ActivityLogBuilder(publisher);
    }
}

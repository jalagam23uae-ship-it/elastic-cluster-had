package com.dynamic.xsd.scheduler;

import com.dynamic.xsd.service.AuditLogService;
import com.dynamic.xsd.service.MetricsCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for cleanup and maintenance.
 *
 * Features:
 * - Old audit log cleanup
 * - Old metrics cleanup
 * - Expired rate limit cleanup
 * - Runs daily at 2 AM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final AuditLogService auditLogService;
    private final MetricsCollectionService metricsCollectionService;

    // Configuration (should be externalized to application.yml)
    private static final int AUDIT_RETENTION_DAYS = 90;
    private static final int METRICS_RETENTION_DAYS = 30;

    /**
     * Cleanup job - runs daily at 2 AM.
     */
    @Scheduled(cron = "${cleanup.scheduler.cron:0 0 2 * * ?}")
    public void performCleanup() {
        log.info("Starting scheduled cleanup job");

        try {
            // Clean old audit logs
            int auditLogsDeleted = auditLogService.deleteOldAuditLogs(AUDIT_RETENTION_DAYS);
            log.info("Deleted {} old audit log entries", auditLogsDeleted);

            // Clean old metrics
            int metricsDeleted = metricsCollectionService.deleteOldMetrics(METRICS_RETENTION_DAYS);
            log.info("Deleted {} old metric entries", metricsDeleted);

            log.info("Scheduled cleanup job completed successfully");

        } catch (Exception e) {
            log.error("Scheduled cleanup job failed", e);
        }
    }

    /**
     * Health check metrics - runs every minute.
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void collectHealthMetrics() {
        try {
            // Record system metrics
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double usedMemoryMB = usedMemory / (1024.0 * 1024.0);

            log.debug("System memory usage: {} MB", String.format("%.2f", usedMemoryMB));

            // Can record to metrics service if needed
            // metricsCollectionService.recordMetric("system", MetricType.MEMORY_USAGE, usedMemoryMB, null);

        } catch (Exception e) {
            log.error("Failed to collect health metrics", e);
        }
    }
}

package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.AuditLog;
import com.dynamic.xsd.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing audit logs.
 *
 * Features:
 * - Async audit logging (non-blocking)
 * - Request correlation tracking
 * - Performance tracking (duration)
 * - IP and user agent capture
 * - Search and filtering
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Logs an audit entry asynchronously.
     *
     * @param username Username performing the action
     * @param action Action type
     * @param serviceName Service affected (optional)
     * @param status Operation status
     * @param details Additional details
     */
    @Async
    @Transactional
    public void logAsync(String username, AuditLog.AuditAction action, String serviceName,
                        AuditLog.AuditStatus status, String details) {
        log(username, action, serviceName, status, details, null, null, null, null, null);
    }

    /**
     * Logs an audit entry synchronously.
     */
    @Transactional
    public void log(String username, AuditLog.AuditAction action, String serviceName,
                   AuditLog.AuditStatus status, String details) {
        log(username, action, serviceName, status, details, null, null, null, null, null);
    }

    /**
     * Logs an audit entry with complete information.
     *
     * @param username Username
     * @param action Action type
     * @param serviceName Service name
     * @param status Status
     * @param details Details
     * @param ipAddress Client IP
     * @param userAgent Client user agent
     * @param errorMessage Error message if failed
     * @param requestId Request correlation ID
     * @param durationMs Operation duration in milliseconds
     */
    @Transactional
    public void log(String username, AuditLog.AuditAction action, String serviceName,
                   AuditLog.AuditStatus status, String details, String ipAddress,
                   String userAgent, String errorMessage, String requestId, Long durationMs) {

        try {
            AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .serviceName(serviceName)
                .status(status)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .errorMessage(errorMessage)
                .requestId(requestId != null ? requestId : UUID.randomUUID().toString())
                .durationMs(durationMs)
                .build();

            auditLogRepository.save(auditLog);

            log.debug("Audit log created: {} - {} - {} - {}",
                username, action, serviceName, status);
        } catch (Exception e) {
            // Never fail the main operation due to audit logging failure
            log.error("Failed to create audit log: {} - {}", action, details, e);
        }
    }

    /**
     * Logs from HTTP request context.
     *
     * @param username Username
     * @param action Action type
     * @param serviceName Service name
     * @param status Status
     * @param details Details
     * @param request HTTP request
     * @param startTime Operation start time
     */
    public void logFromRequest(String username, AuditLog.AuditAction action, String serviceName,
                              AuditLog.AuditStatus status, String details,
                              HttpServletRequest request, LocalDateTime startTime) {

        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestId = (String) request.getAttribute("requestId");
        Long durationMs = startTime != null ?
            java.time.Duration.between(startTime, LocalDateTime.now()).toMillis() : null;

        log(username, action, serviceName, status, details, ipAddress, userAgent, null, requestId, durationMs);
    }

    /**
     * Logs a successful operation.
     */
    @Async
    @Transactional
    public void logSuccess(String username, AuditLog.AuditAction action, String serviceName, String details) {
        log(username, action, serviceName, AuditLog.AuditStatus.SUCCESS, details);
    }

    /**
     * Logs a failed operation.
     */
    @Async
    @Transactional
    public void logFailure(String username, AuditLog.AuditAction action, String serviceName,
                          String details, String errorMessage) {
        log(username, action, serviceName, AuditLog.AuditStatus.FAILED, details,
            null, null, errorMessage, null, null);
    }

    /**
     * Gets audit logs with pagination.
     */
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Gets audit logs for a specific user.
     */
    public Page<AuditLog> getAuditLogsByUsername(String username, Pageable pageable) {
        return auditLogRepository.findByUsername(username, pageable);
    }

    /**
     * Gets audit logs for a specific action.
     */
    public Page<AuditLog> getAuditLogsByAction(AuditLog.AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    /**
     * Gets audit logs for a specific service.
     */
    public List<AuditLog> getAuditLogsByServiceName(String serviceName) {
        return auditLogRepository.findByServiceName(serviceName);
    }

    /**
     * Gets audit logs within a time range.
     */
    public List<AuditLog> getAuditLogsBetween(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }

    /**
     * Gets recent failed operations.
     */
    public List<AuditLog> getRecentFailures(int limit) {
        return auditLogRepository.findByStatusOrderByTimestampDesc(
            AuditLog.AuditStatus.FAILED,
            org.springframework.data.domain.PageRequest.of(0, limit)
        ).getContent();
    }

    /**
     * Gets audit statistics for a user.
     */
    public AuditStatistics getUserStatistics(String username) {
        long totalActions = auditLogRepository.countByUsername(username);
        long successfulActions = auditLogRepository.countByUsernameAndStatus(username, AuditLog.AuditStatus.SUCCESS);
        long failedActions = auditLogRepository.countByUsernameAndStatus(username, AuditLog.AuditStatus.FAILED);

        return AuditStatistics.builder()
            .username(username)
            .totalActions(totalActions)
            .successfulActions(successfulActions)
            .failedActions(failedActions)
            .successRate(totalActions > 0 ? (successfulActions * 100.0 / totalActions) : 0)
            .build();
    }

    /**
     * Deletes old audit logs.
     *
     * @param retentionDays Number of days to retain
     * @return Number of deleted records
     */
    @Transactional
    public int deleteOldAuditLogs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        log.info("Deleting audit logs older than {} days (before {})", retentionDays, cutoffDate);

        List<AuditLog> oldLogs = auditLogRepository.findByTimestampBefore(cutoffDate);
        int count = oldLogs.size();

        if (count > 0) {
            auditLogRepository.deleteAll(oldLogs);
            log.info("Deleted {} old audit log entries", count);
        }

        return count;
    }

    /**
     * Extracts IP address from request, handling proxies.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs (proxy chain), take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Audit statistics response.
     */
    @lombok.Builder
    @lombok.Data
    public static class AuditStatistics {
        private String username;
        private long totalActions;
        private long successfulActions;
        private long failedActions;
        private double successRate;
    }
}

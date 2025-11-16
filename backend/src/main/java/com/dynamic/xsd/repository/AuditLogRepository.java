package com.dynamic.xsd.repository;

import com.dynamic.xsd.domain.entity.AuditLog;
import com.dynamic.xsd.domain.entity.AuditLog.AuditAction;
import com.dynamic.xsd.domain.entity.AuditLog.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByUsername(String username);

    Page<AuditLog> findByUsername(String username, Pageable pageable);

    List<AuditLog> findByAction(AuditAction action);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    List<AuditLog> findByStatus(AuditStatus status);

    List<AuditLog> findByServiceName(String serviceName);

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByStatusOrderByTimestampDesc(AuditStatus status, Pageable pageable);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByTimestampBefore(LocalDateTime timestamp);

    Long countByUsername(String username);

    Long countByUsernameAndStatus(String username, AuditStatus status);

    @Query("SELECT a FROM AuditLog a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentLogs(LocalDateTime since);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:username IS NULL OR a.username = :username) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:status IS NULL OR a.status = :status) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByFilters(String username, AuditAction action, AuditStatus status, Pageable pageable);
}

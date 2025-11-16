package com.dynamic.xsd.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing audit log entries.
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_user", columnList = "username")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status;

    @Column(length = 2000)
    private String details;

    @Column
    private String ipAddress;

    @Column(length = 5000)
    private String errorMessage;

    public enum AuditAction {
        SCHEMA_UPLOAD,
        SCHEMA_DELETE,
        SCHEMA_UPDATE,
        SERVICE_DEPLOY,
        SERVICE_UNDEPLOY,
        SERVICE_UPDATE,
        USER_LOGIN,
        USER_LOGOUT,
        USER_CREATE,
        USER_UPDATE,
        USER_DELETE,
        CONFIG_UPDATE
    }

    public enum AuditStatus {
        SUCCESS,
        FAILED
    }
}

package com.dynamic.xsd.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a deployed service definition.
 */
@Entity
@Table(name = "service_definition", indexes = {
    @Index(name = "idx_service_name_svc", columnList = "serviceName"),
    @Index(name = "idx_status_svc", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String serviceName;

    @Column(nullable = false)
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_id", referencedColumnName = "id", nullable = false)
    private SchemaMetadata schemaMetadata;

    @Column(columnDefinition = "TEXT")
    private String wsdlContent;

    @Column
    private String serviceVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.DEPLOYING;

    @Column
    private LocalDateTime deployedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime undeployedAt;

    @Column
    private LocalDateTime lastHealthCheck;

    @Column
    private String deployedBy;

    @OneToMany(mappedBy = "serviceDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EndpointMapping> endpoints = new ArrayList<>();

    @Column
    private Long totalRequests;

    @Column
    private Long successfulRequests;

    @Column
    private Long failedRequests;

    @Column
    private Double averageResponseTime;

    @Column(length = 2000)
    private String deploymentErrors;

    public enum ServiceStatus {
        DEPLOYING,
        DEPLOYED,
        DEPLOYMENT_FAILED,
        UNDEPLOYING,
        UNDEPLOYED,
        FAILED
    }

    public void incrementTotalRequests() {
        this.totalRequests = (this.totalRequests == null ? 0 : this.totalRequests) + 1;
    }

    public void incrementSuccessfulRequests() {
        this.successfulRequests = (this.successfulRequests == null ? 0 : this.successfulRequests) + 1;
    }

    public void incrementFailedRequests() {
        this.failedRequests = (this.failedRequests == null ? 0 : this.failedRequests) + 1;
    }
}

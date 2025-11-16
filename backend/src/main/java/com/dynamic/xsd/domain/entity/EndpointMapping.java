package com.dynamic.xsd.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing endpoint mappings for generated services.
 */
@Entity
@Table(name = "endpoint_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_definition_id", nullable = false)
    private ServiceDefinition serviceDefinition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EndpointType type;

    @Column(nullable = false)
    private String path;

    @Column
    private String httpMethod;  // For REST endpoints (GET, POST, PUT, DELETE, PATCH)

    @Column
    private String operationName;  // For SOAP operations

    @Column
    private String description;

    @Column(nullable = false)
    private Boolean active;

    public enum EndpointType {
        REST,
        SOAP
    }
}

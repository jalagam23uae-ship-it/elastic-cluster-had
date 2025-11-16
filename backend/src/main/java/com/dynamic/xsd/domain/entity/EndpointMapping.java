package com.dynamic.xsd.domain.entity;

import com.dynamic.xsd.domain.enums.EndpointType;
import com.dynamic.xsd.domain.enums.HttpMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
    private EndpointType endpointType;

    @Column(nullable = false)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column
    private HttpMethod httpMethod;  // For REST endpoints (GET, POST, PUT, DELETE, PATCH)

    @Column
    private String operationName;  // For SOAP operations

    @Column
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Backward compatibility aliases
    public EndpointType getType() {
        return endpointType;
    }

    public void setType(EndpointType type) {
        this.endpointType = type;
    }
}

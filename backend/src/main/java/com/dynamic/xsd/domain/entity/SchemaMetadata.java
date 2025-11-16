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
 * Entity representing XSD schema metadata.
 */
@Entity
@Table(name = "schema_metadata", indexes = {
    @Index(name = "idx_service_name", columnList = "serviceName"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String serviceName;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String namespace;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SchemaStatus status = SchemaStatus.UPLOADED;

    @Column(nullable = false)
    private String uploadedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String xsdContent;

    @Column
    private String xsdFilePath;

    @ElementCollection
    @CollectionTable(name = "generated_pojos", joinColumns = @JoinColumn(name = "schema_id"))
    @Column(name = "pojo_class_name")
    @Builder.Default
    private List<String> generatedPojos = new ArrayList<>();

    @Column
    private Integer restEndpointCount;

    @Column
    private Integer soapOperationCount;

    @Column(length = 2000)
    private String compilationErrors;

    @Column(length = 2000)
    private String validationErrors;

    public enum SchemaStatus {
        UPLOADED,
        VALIDATING,
        VALIDATION_FAILED,
        GENERATING,
        GENERATION_FAILED,
        COMPILING,
        COMPILATION_FAILED,
        ACTIVE,
        FAILED,
        DEPRECATED
    }
}

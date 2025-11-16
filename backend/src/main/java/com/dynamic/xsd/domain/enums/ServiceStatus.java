package com.dynamic.xsd.domain.enums;

/**
 * General service status enum used across the platform.
 * Maps to both SchemaMetadata.SchemaStatus and ServiceDefinition.ServiceStatus.
 */
public enum ServiceStatus {
    // Schema statuses
    UPLOADED,
    VALIDATING,
    VALIDATION_FAILED,
    GENERATING,
    GENERATION_FAILED,
    COMPILING,
    COMPILATION_FAILED,

    // Service deployment statuses
    DEPLOYING,
    DEPLOYED,
    DEPLOYMENT_FAILED,
    UNDEPLOYING,
    UNDEPLOYED,

    // Common statuses
    ACTIVE,
    INACTIVE,
    FAILED,
    DEPRECATED,
    PROCESSING
}

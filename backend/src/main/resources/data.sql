-- =====================================================
-- Dynamic XSD Service Generation Platform
-- Initial Seed Data for Development
-- =====================================================
-- Version: 1.0
-- Created: 2025-11-16
-- Description: Default users and configuration data
-- =====================================================

-- =====================================================
-- USERS: Default admin and test users
-- =====================================================
-- Password: admin123 (BCrypt hash with strength 10)
-- IMPORTANT: Change these credentials in production!
INSERT INTO users (id, username, email, password, role, active, email_verified)
VALUES (
    '00000000-0000-0000-0000-000000000001'::UUID,
    'admin',
    'admin@dynamicxsd.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    true,
    true
) ON CONFLICT (username) DO NOTHING;

-- Password: user123 (BCrypt hash with strength 10)
INSERT INTO users (id, username, email, password, role, active, email_verified)
VALUES (
    '00000000-0000-0000-0000-000000000002'::UUID,
    'developer',
    'developer@dynamicxsd.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'USER',
    true,
    true
) ON CONFLICT (username) DO NOTHING;

-- Password: viewer123 (BCrypt hash with strength 10)
INSERT INTO users (id, username, email, password, role, active, email_verified)
VALUES (
    '00000000-0000-0000-0000-000000000003'::UUID,
    'viewer',
    'viewer@dynamicxsd.com',
    '$2a$10$E0s5TwYLmKw6gKOzKmP7meNvPvBvI3yLqDxMPiZkJq1c3I0YFqD7i',
    'VIEWER',
    true,
    true
) ON CONFLICT (username) DO NOTHING;

-- =====================================================
-- CONFIGURATION: Default platform settings
-- =====================================================

-- Maximum XSD file size (10MB)
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'xsd.max.file.size',
    '10485760',
    'INTEGER',
    'Maximum XSD file size in bytes (10MB)',
    'XSD_PROCESSING',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- XSD storage path
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'xsd.storage.path',
    './xsd-storage',
    'STRING',
    'Directory path for storing uploaded XSD files',
    'XSD_PROCESSING',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Generated code output path
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'codegen.output.path',
    './generated',
    'STRING',
    'Base directory for generated code',
    'CODE_GENERATION',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Compilation temp path
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'compiler.temp.path',
    './temp',
    'STRING',
    'Temporary directory for compilation',
    'CODE_GENERATION',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- ClassLoader isolation enabled
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'classloader.isolation.enabled',
    'true',
    'BOOLEAN',
    'Enable classloader isolation for services',
    'CLASSLOADER',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- JWT secret (CHANGE IN PRODUCTION!)
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'jwt.secret',
    'your-256-bit-secret-change-this-in-production-minimum-32-characters',
    'STRING',
    'JWT signing secret key',
    'SECURITY',
    true
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- JWT expiration (24 hours)
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'jwt.expiration.ms',
    '86400000',
    'INTEGER',
    'JWT token expiration time in milliseconds (24 hours)',
    'SECURITY',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Rate limit: requests per minute
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'rate.limit.requests.per.minute',
    '100',
    'INTEGER',
    'Maximum requests per minute per user',
    'RATE_LIMITING',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Rate limit: burst size
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'rate.limit.burst.size',
    '20',
    'INTEGER',
    'Maximum burst size for rate limiting',
    'RATE_LIMITING',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Metrics retention days
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'metrics.retention.days',
    '30',
    'INTEGER',
    'Number of days to retain metrics data',
    'METRICS',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Audit log retention days
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'audit.retention.days',
    '90',
    'INTEGER',
    'Number of days to retain audit logs',
    'AUDIT',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Enable REST endpoints by default
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'endpoint.rest.enabled',
    'true',
    'BOOLEAN',
    'Enable REST endpoint generation by default',
    'ENDPOINTS',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Enable SOAP endpoints by default
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'endpoint.soap.enabled',
    'true',
    'BOOLEAN',
    'Enable SOAP endpoint generation by default',
    'ENDPOINTS',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Maximum concurrent compilations
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'compiler.max.concurrent',
    '4',
    'INTEGER',
    'Maximum concurrent compilation tasks',
    'CODE_GENERATION',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Health check interval (seconds)
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'healthcheck.interval.seconds',
    '60',
    'INTEGER',
    'Interval for service health checks in seconds',
    'MONITORING',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Enable detailed logging
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'logging.detailed.enabled',
    'false',
    'BOOLEAN',
    'Enable detailed debug logging',
    'LOGGING',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Cleanup scheduler cron
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'cleanup.scheduler.cron',
    '0 0 2 * * ?',
    'STRING',
    'Cron expression for cleanup scheduler (2 AM daily)',
    'MAINTENANCE',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Maximum schema versions to keep
INSERT INTO configuration (key, value, value_type, description, category, is_sensitive)
VALUES (
    'schema.max.versions',
    '10',
    'INTEGER',
    'Maximum number of schema versions to keep per service',
    'SCHEMA_MANAGEMENT',
    false
) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- =====================================================
-- INITIAL AUDIT LOG ENTRY
-- =====================================================
INSERT INTO audit_log (username, action, status, details)
VALUES (
    'system',
    'CONFIG_CHANGE',
    'SUCCESS',
    'Database initialized with default configuration'
);

-- =====================================================
-- DEVELOPMENT HELPER: Sample XSD Schema (Optional)
-- =====================================================
-- Uncomment to insert a sample schema for testing

/*
INSERT INTO schema_metadata (
    id,
    service_name,
    version,
    namespace,
    target_namespace,
    package_name,
    description,
    status,
    uploaded_by,
    xsd_content
)
VALUES (
    '10000000-0000-0000-0000-000000000001'::UUID,
    'sample-service',
    '1.0.0',
    'http://example.com/sample',
    'http://example.com/sample',
    'com.generated.sampleservice.model',
    'Sample XSD schema for testing',
    'ACTIVE',
    'admin',
    '<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           targetNamespace="http://example.com/sample"
           elementFormDefault="qualified">
    <xs:element name="Person">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="name" type="xs:string"/>
                <xs:element name="age" type="xs:int"/>
                <xs:element name="email" type="xs:string"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>
</xs:schema>'
) ON CONFLICT (service_name) DO NOTHING;
*/

-- =====================================================
-- END OF SEED DATA
-- =====================================================

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Seed data loaded successfully';
    RAISE NOTICE 'Default users created:';
    RAISE NOTICE '  - admin / admin123 (ADMIN)';
    RAISE NOTICE '  - developer / user123 (USER)';
    RAISE NOTICE '  - viewer / viewer123 (VIEWER)';
    RAISE NOTICE 'IMPORTANT: Change default passwords in production!';
END $$;

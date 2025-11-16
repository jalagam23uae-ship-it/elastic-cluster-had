-- =====================================================
-- Dynamic XSD Service Generation Platform
-- Database Schema for PostgreSQL with R2DBC
-- =====================================================
-- Version: 1.0
-- Created: 2025-11-16
-- Description: Complete schema for XSD-to-Service platform
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- TABLE: users
-- Purpose: User accounts and authentication
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'USER', 'VIEWER')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked BOOLEAN DEFAULT FALSE,
    account_locked_until TIMESTAMP,
    password_changed_at TIMESTAMP,
    email_verified BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE users IS 'User accounts and authentication information';
COMMENT ON COLUMN users.role IS 'User role: ADMIN, USER, or VIEWER';
COMMENT ON COLUMN users.account_locked IS 'Account locked due to failed login attempts';
COMMENT ON COLUMN users.failed_login_attempts IS 'Counter for consecutive failed login attempts';

-- =====================================================
-- TABLE: schema_metadata
-- Purpose: XSD schema metadata and processing status
-- =====================================================
CREATE TABLE IF NOT EXISTS schema_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(255) UNIQUE NOT NULL,
    version VARCHAR(50) NOT NULL,
    namespace VARCHAR(500),
    target_namespace VARCHAR(500) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(50) NOT NULL CHECK (status IN (
        'UPLOADED', 'VALIDATING', 'VALIDATION_FAILED',
        'GENERATING', 'GENERATION_FAILED',
        'COMPILING', 'COMPILATION_FAILED',
        'ACTIVE', 'FAILED', 'DEPRECATED'
    )),
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    xsd_content TEXT,
    xsd_file_path VARCHAR(1000),
    source_output_path VARCHAR(1000),
    class_output_path VARCHAR(1000),
    rest_endpoint_count INTEGER DEFAULT 0,
    soap_operation_count INTEGER DEFAULT 0,
    compilation_errors TEXT,
    validation_errors TEXT,
    file_size_bytes BIGINT,
    checksum VARCHAR(64)
);

COMMENT ON TABLE schema_metadata IS 'XSD schema metadata and processing lifecycle';
COMMENT ON COLUMN schema_metadata.status IS 'Processing status of the schema';
COMMENT ON COLUMN schema_metadata.target_namespace IS 'Target namespace from XSD for WSDL generation';
COMMENT ON COLUMN schema_metadata.package_name IS 'Java package name for generated classes';
COMMENT ON COLUMN schema_metadata.class_output_path IS 'Directory path for compiled .class files';
COMMENT ON COLUMN schema_metadata.checksum IS 'SHA-256 checksum of XSD content';

-- =====================================================
-- TABLE: generated_pojos
-- Purpose: List of generated POJO class names per schema
-- =====================================================
CREATE TABLE IF NOT EXISTS generated_pojos (
    schema_id UUID NOT NULL REFERENCES schema_metadata(id) ON DELETE CASCADE,
    pojo_class_name VARCHAR(500) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    has_xml_root_element BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (schema_id, pojo_class_name)
);

COMMENT ON TABLE generated_pojos IS 'Generated POJO classes from XSD schemas';
COMMENT ON COLUMN generated_pojos.has_xml_root_element IS 'Whether class has @XmlRootElement annotation';

-- =====================================================
-- TABLE: service_definition
-- Purpose: Deployed service metadata and metrics
-- =====================================================
CREATE TABLE IF NOT EXISTS service_definition (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(255) UNIQUE NOT NULL,
    version VARCHAR(50) NOT NULL,
    schema_id UUID NOT NULL REFERENCES schema_metadata(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL CHECK (status IN (
        'DEPLOYING', 'DEPLOYED', 'DEPLOYMENT_FAILED',
        'UNDEPLOYING', 'UNDEPLOYED', 'FAILED'
    )),
    deployed_at TIMESTAMP,
    updated_at TIMESTAMP,
    undeployed_at TIMESTAMP,
    last_health_check TIMESTAMP,
    deployed_by VARCHAR(255),
    wsdl_content TEXT,
    service_version VARCHAR(50),
    total_requests BIGINT DEFAULT 0,
    successful_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    average_response_time DOUBLE PRECISION,
    last_request_at TIMESTAMP,
    deployment_errors TEXT
);

COMMENT ON TABLE service_definition IS 'Deployed service definitions and runtime metrics';
COMMENT ON COLUMN service_definition.status IS 'Deployment status';
COMMENT ON COLUMN service_definition.wsdl_content IS 'Generated WSDL document for SOAP services';
COMMENT ON COLUMN service_definition.average_response_time IS 'Average response time in milliseconds';

-- =====================================================
-- TABLE: endpoint_mapping
-- Purpose: REST and SOAP endpoint mappings
-- =====================================================
CREATE TABLE IF NOT EXISTS endpoint_mapping (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_definition_id UUID NOT NULL REFERENCES service_definition(id) ON DELETE CASCADE,
    endpoint_type VARCHAR(20) NOT NULL CHECK (endpoint_type IN ('REST', 'SOAP')),
    path VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) CHECK (http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH')),
    operation_name VARCHAR(255),
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_count BIGINT DEFAULT 0,
    last_accessed_at TIMESTAMP
);

COMMENT ON TABLE endpoint_mapping IS 'REST and SOAP endpoint mappings for deployed services';
COMMENT ON COLUMN endpoint_mapping.endpoint_type IS 'Type: REST or SOAP';
COMMENT ON COLUMN endpoint_mapping.http_method IS 'HTTP method for REST endpoints';
COMMENT ON COLUMN endpoint_mapping.operation_name IS 'Operation name for SOAP endpoints';

-- =====================================================
-- TABLE: audit_log
-- Purpose: Audit trail for all platform operations
-- =====================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL CHECK (action IN (
        'SCHEMA_UPLOAD', 'SCHEMA_DELETE', 'SCHEMA_UPDATE',
        'SERVICE_DEPLOY', 'SERVICE_UNDEPLOY', 'SERVICE_REDEPLOY',
        'USER_LOGIN', 'USER_LOGOUT', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE',
        'ENDPOINT_ACCESS', 'CONFIG_CHANGE'
    )),
    service_name VARCHAR(255),
    status VARCHAR(50) CHECK (status IN ('SUCCESS', 'FAILED', 'PARTIAL')),
    details VARCHAR(2000),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    error_message TEXT,
    request_id UUID,
    duration_ms BIGINT
);

COMMENT ON TABLE audit_log IS 'Comprehensive audit trail of platform operations';
COMMENT ON COLUMN audit_log.action IS 'Type of action performed';
COMMENT ON COLUMN audit_log.duration_ms IS 'Duration of operation in milliseconds';

-- =====================================================
-- TABLE: service_metrics
-- Purpose: Time-series metrics for services
-- =====================================================
CREATE TABLE IF NOT EXISTS service_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_definition_id UUID NOT NULL REFERENCES service_definition(id) ON DELETE CASCADE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metric_type VARCHAR(50) NOT NULL CHECK (metric_type IN (
        'REQUEST_COUNT', 'RESPONSE_TIME', 'ERROR_RATE',
        'THROUGHPUT', 'CPU_USAGE', 'MEMORY_USAGE'
    )),
    metric_value DOUBLE PRECISION NOT NULL,
    tags JSONB,
    aggregation_window VARCHAR(20) CHECK (aggregation_window IN ('1m', '5m', '15m', '1h', '1d'))
);

COMMENT ON TABLE service_metrics IS 'Time-series metrics for deployed services';
COMMENT ON COLUMN service_metrics.metric_type IS 'Type of metric being recorded';
COMMENT ON COLUMN service_metrics.tags IS 'Additional metadata as JSON';

-- =====================================================
-- TABLE: rate_limit_buckets
-- Purpose: Rate limiting token buckets per user/service
-- =====================================================
CREATE TABLE IF NOT EXISTS rate_limit_buckets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identifier VARCHAR(255) NOT NULL,
    bucket_type VARCHAR(50) NOT NULL CHECK (bucket_type IN ('USER', 'SERVICE', 'IP', 'GLOBAL')),
    tokens_remaining INTEGER NOT NULL,
    max_tokens INTEGER NOT NULL,
    refill_rate INTEGER NOT NULL,
    last_refill_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    window_reset_at TIMESTAMP NOT NULL,
    UNIQUE(identifier, bucket_type)
);

COMMENT ON TABLE rate_limit_buckets IS 'Token bucket rate limiting state';
COMMENT ON COLUMN rate_limit_buckets.identifier IS 'User ID, service name, or IP address';
COMMENT ON COLUMN rate_limit_buckets.refill_rate IS 'Tokens per second refill rate';

-- =====================================================
-- TABLE: configuration
-- Purpose: Platform configuration key-value store
-- =====================================================
CREATE TABLE IF NOT EXISTS configuration (
    key VARCHAR(255) PRIMARY KEY,
    value TEXT NOT NULL,
    value_type VARCHAR(50) NOT NULL CHECK (value_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'JSON')),
    description VARCHAR(1000),
    category VARCHAR(100),
    is_sensitive BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    updated_by VARCHAR(255)
);

COMMENT ON TABLE configuration IS 'Platform configuration settings';
COMMENT ON COLUMN configuration.is_sensitive IS 'Whether value contains sensitive data';

-- =====================================================
-- INDEXES for Performance Optimization
-- =====================================================

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);

-- Schema metadata indexes
CREATE INDEX IF NOT EXISTS idx_schema_service_name ON schema_metadata(service_name);
CREATE INDEX IF NOT EXISTS idx_schema_status ON schema_metadata(status);
CREATE INDEX IF NOT EXISTS idx_schema_uploaded_by ON schema_metadata(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_schema_uploaded_at ON schema_metadata(uploaded_at DESC);

-- Service definition indexes
CREATE INDEX IF NOT EXISTS idx_service_name_svc ON service_definition(service_name);
CREATE INDEX IF NOT EXISTS idx_service_schema_id ON service_definition(schema_id);
CREATE INDEX IF NOT EXISTS idx_status_svc ON service_definition(status);
CREATE INDEX IF NOT EXISTS idx_service_deployed_at ON service_definition(deployed_at DESC);

-- Endpoint mapping indexes
CREATE INDEX IF NOT EXISTS idx_endpoint_service_def_id ON endpoint_mapping(service_definition_id);
CREATE INDEX IF NOT EXISTS idx_endpoint_type ON endpoint_mapping(endpoint_type);
CREATE INDEX IF NOT EXISTS idx_endpoint_path ON endpoint_mapping(path);
CREATE INDEX IF NOT EXISTS idx_endpoint_active ON endpoint_mapping(active);

-- Audit log indexes
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_username ON audit_log(username);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_service_name ON audit_log(service_name);
CREATE INDEX IF NOT EXISTS idx_audit_status ON audit_log(status);

-- Service metrics indexes
CREATE INDEX IF NOT EXISTS idx_metrics_service_id ON service_metrics(service_definition_id);
CREATE INDEX IF NOT EXISTS idx_metrics_timestamp ON service_metrics(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_metrics_type ON service_metrics(metric_type);

-- Rate limit indexes
CREATE INDEX IF NOT EXISTS idx_rate_limit_identifier ON rate_limit_buckets(identifier);
CREATE INDEX IF NOT EXISTS idx_rate_limit_type ON rate_limit_buckets(bucket_type);

-- =====================================================
-- VIEWS for Common Queries
-- =====================================================

-- View: Active services with schema info
CREATE OR REPLACE VIEW v_active_services AS
SELECT
    sd.id,
    sd.service_name,
    sd.version,
    sd.status,
    sd.deployed_at,
    sd.total_requests,
    sd.successful_requests,
    sd.failed_requests,
    sd.average_response_time,
    sm.uploaded_by,
    sm.target_namespace,
    (SELECT COUNT(*) FROM endpoint_mapping em WHERE em.service_definition_id = sd.id AND em.active = true) as active_endpoints
FROM service_definition sd
JOIN schema_metadata sm ON sd.schema_id = sm.id
WHERE sd.status = 'DEPLOYED';

COMMENT ON VIEW v_active_services IS 'Quick view of all deployed services with metrics';

-- View: Schema processing summary
CREATE OR REPLACE VIEW v_schema_summary AS
SELECT
    sm.id,
    sm.service_name,
    sm.version,
    sm.status,
    sm.uploaded_at,
    sm.uploaded_by,
    (SELECT COUNT(*) FROM generated_pojos gp WHERE gp.schema_id = sm.id) as pojo_count,
    CASE
        WHEN EXISTS (SELECT 1 FROM service_definition sd WHERE sd.schema_id = sm.id AND sd.status = 'DEPLOYED')
        THEN 'DEPLOYED'
        ELSE 'NOT_DEPLOYED'
    END as deployment_status
FROM schema_metadata sm;

COMMENT ON VIEW v_schema_summary IS 'Summary view of schemas with deployment status';

-- View: Recent audit activity
CREATE OR REPLACE VIEW v_recent_audit AS
SELECT
    al.timestamp,
    al.username,
    al.action,
    al.service_name,
    al.status,
    al.duration_ms,
    u.role as user_role
FROM audit_log al
LEFT JOIN users u ON al.username = u.username
ORDER BY al.timestamp DESC
LIMIT 100;

COMMENT ON VIEW v_recent_audit IS 'Most recent 100 audit log entries';

-- =====================================================
-- FUNCTIONS for Common Operations
-- =====================================================

-- Function: Update service metrics
CREATE OR REPLACE FUNCTION update_service_metrics()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function: Calculate success rate
CREATE OR REPLACE FUNCTION calculate_success_rate(p_service_id UUID)
RETURNS DOUBLE PRECISION AS $$
DECLARE
    v_total BIGINT;
    v_successful BIGINT;
BEGIN
    SELECT total_requests, successful_requests
    INTO v_total, v_successful
    FROM service_definition
    WHERE id = p_service_id;

    IF v_total = 0 THEN
        RETURN 0;
    END IF;

    RETURN (v_successful::DOUBLE PRECISION / v_total::DOUBLE PRECISION) * 100;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_success_rate IS 'Calculate success rate percentage for a service';

-- =====================================================
-- TRIGGERS
-- =====================================================

-- Trigger: Auto-update updated_at on service_definition
CREATE TRIGGER trg_service_definition_updated
    BEFORE UPDATE ON service_definition
    FOR EACH ROW
    EXECUTE FUNCTION update_service_metrics();

-- Trigger: Auto-update updated_at on schema_metadata
CREATE TRIGGER trg_schema_metadata_updated
    BEFORE UPDATE ON schema_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_service_metrics();

-- =====================================================
-- INITIAL DATA GRANTS (Optional - for production)
-- =====================================================
-- Grant permissions to application user (uncomment in production)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO xsd_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO xsd_app_user;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO xsd_app_user;

-- =====================================================
-- SCHEMA VERSION TRACKING
-- =====================================================
CREATE TABLE IF NOT EXISTS schema_version (
    version VARCHAR(20) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(500)
);

INSERT INTO schema_version (version, description)
VALUES ('1.0.0', 'Initial schema creation with core tables')
ON CONFLICT (version) DO NOTHING;

-- =====================================================
-- END OF SCHEMA
-- =====================================================

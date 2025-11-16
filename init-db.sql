-- Database initialization script for XSD Service Platform
-- This script is automatically executed when PostgreSQL container starts for the first time

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create database (already created by POSTGRES_DB env var, but this is for documentation)
-- The database 'xsdplatform' is created automatically

-- Schema is created by Hibernate, but you can add custom initialization here

-- Insert default admin user (password: admin123, will be hashed by application)
-- This is just for reference, actual user creation should be done through the application

-- Sample data or lookup tables can be inserted here
-- For example:

-- CREATE TABLE IF NOT EXISTS system_config (
--     key VARCHAR(255) PRIMARY KEY,
--     value TEXT,
--     description TEXT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );

-- INSERT INTO system_config (key, value, description) VALUES
-- ('max_upload_size', '5242880', 'Maximum XSD file upload size in bytes'),
-- ('default_page_size', '20', 'Default pagination size for list APIs');

-- Grant permissions if needed
-- GRANT ALL PRIVILEGES ON DATABASE xsdplatform TO postgres;

-- Create indexes for performance (Hibernate will create these, but you can optimize here)
-- CREATE INDEX IF NOT EXISTS idx_schema_service_name ON schema_metadata(service_name);
-- CREATE INDEX IF NOT EXISTS idx_schema_status ON schema_metadata(status);
-- CREATE INDEX IF NOT EXISTS idx_service_status ON service_definition(status);
-- CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp);

-- Print success message
DO $$
BEGIN
    RAISE NOTICE 'XSD Service Platform database initialized successfully';
END $$;

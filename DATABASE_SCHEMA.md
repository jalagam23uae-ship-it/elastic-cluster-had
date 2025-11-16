# Database Schema Documentation

**Dynamic XSD Service Generation Platform**
**Version:** 1.0
**Last Updated:** 2025-11-16

---

## Overview

This document describes the database schema for the Dynamic XSD Service Generation Platform. The schema is designed to work with both **JPA (Hibernate)** and **R2DBC (reactive)** implementations.

### Database: PostgreSQL 15+

The platform uses PostgreSQL with the following features:
- UUID primary keys via `uuid-ossp` extension
- JSONB columns for flexible metadata
- Triggers for automatic timestamp updates
- Views for common query patterns
- Functions for business logic

---

## Schema Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CORE ENTITIES                             │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐         ┌──────────────────┐             │
│  │    users     │         │ schema_metadata  │             │
│  └──────┬───────┘         └────────┬─────────┘             │
│         │                           │                        │
│         │                           │ 1                      │
│         │                           │                        │
│         │                   ┌───────▼──────────┐            │
│         │                   │ generated_pojos  │            │
│         │                   └──────────────────┘            │
│         │                           │                        │
│         │                           │ 1                      │
│         │                           │                        │
│         │                   ┌───────▼────────────┐          │
│         │                   │ service_definition │          │
│         │                   └───────┬────────────┘          │
│         │                           │                        │
│         │                           │ 1                      │
│         │                           │                        │
│         │                   ┌───────▼──────────────┐        │
│         │                   │  endpoint_mapping    │        │
│         │                   └──────────────────────┘        │
│         │                           │                        │
│         │                           │ n                      │
│         │                           │                        │
│         ├───────────────────────────┴────────────┐          │
│         │                                        │          │
│         ▼                                        ▼          │
│  ┌──────────────┐                    ┌──────────────────┐  │
│  │  audit_log   │                    │ service_metrics  │  │
│  └──────────────┘                    └──────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                  SUPPORTING TABLES                            │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────┐        ┌───────────────┐           │
│  │ rate_limit_buckets  │        │ configuration │           │
│  └─────────────────────┘        └───────────────┘           │
│                                                               │
│  ┌─────────────────────┐                                     │
│  │  schema_version     │                                     │
│  └─────────────────────┘                                     │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## Core Tables

### 1. `users`
**Purpose:** User accounts and authentication

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | User unique identifier |
| username | VARCHAR(100) | UNIQUE, NOT NULL | Login username |
| email | VARCHAR(255) | UNIQUE | Email address |
| password | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| role | VARCHAR(50) | NOT NULL | ADMIN, USER, VIEWER |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE | Account active status |
| created_at | TIMESTAMP | NOT NULL | Account creation timestamp |
| updated_at | TIMESTAMP | | Last update timestamp |
| last_login_at | TIMESTAMP | | Last successful login |
| failed_login_attempts | INTEGER | DEFAULT 0 | Failed login counter |
| account_locked | BOOLEAN | DEFAULT FALSE | Lock status |
| account_locked_until | TIMESTAMP | | Unlock timestamp |
| password_changed_at | TIMESTAMP | | Last password change |
| email_verified | BOOLEAN | DEFAULT FALSE | Email verification status |

**Indexes:**
- `idx_users_username` on `username`
- `idx_users_email` on `email`
- `idx_users_role` on `role`
- `idx_users_active` on `active`

**Default Users:**
- `admin` / `admin123` (ADMIN role)
- `developer` / `user123` (USER role)
- `viewer` / `viewer123` (VIEWER role)

---

### 2. `schema_metadata`
**Purpose:** XSD schema metadata and processing lifecycle

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Schema unique identifier |
| service_name | VARCHAR(255) | UNIQUE, NOT NULL | Service name |
| version | VARCHAR(50) | NOT NULL | Schema version |
| namespace | VARCHAR(500) | | XML namespace |
| target_namespace | VARCHAR(500) | NOT NULL | Target namespace for WSDL |
| package_name | VARCHAR(255) | NOT NULL | Java package name |
| description | VARCHAR(1000) | | Schema description |
| status | VARCHAR(50) | NOT NULL | Processing status |
| uploaded_by | VARCHAR(255) | NOT NULL | Uploader username |
| uploaded_at | TIMESTAMP | NOT NULL | Upload timestamp |
| updated_at | TIMESTAMP | | Last update timestamp |
| xsd_content | TEXT | | XSD file content |
| xsd_file_path | VARCHAR(1000) | | File system path |
| source_output_path | VARCHAR(1000) | | Generated source path |
| class_output_path | VARCHAR(1000) | | Compiled class path |
| rest_endpoint_count | INTEGER | DEFAULT 0 | Number of REST endpoints |
| soap_operation_count | INTEGER | DEFAULT 0 | Number of SOAP operations |
| compilation_errors | TEXT | | Compilation error messages |
| validation_errors | TEXT | | Validation error messages |
| file_size_bytes | BIGINT | | Original file size |
| checksum | VARCHAR(64) | | SHA-256 checksum |

**Status Values:**
- `UPLOADED` - Initial upload
- `VALIDATING` - XSD validation in progress
- `VALIDATION_FAILED` - XSD validation failed
- `GENERATING` - POJO generation in progress
- `GENERATION_FAILED` - POJO generation failed
- `COMPILING` - Compilation in progress
- `COMPILATION_FAILED` - Compilation failed
- `ACTIVE` - Successfully processed and ready
- `FAILED` - General failure
- `DEPRECATED` - No longer in use

**Indexes:**
- `idx_schema_service_name` on `service_name`
- `idx_schema_status` on `status`
- `idx_schema_uploaded_by` on `uploaded_by`
- `idx_schema_uploaded_at` on `uploaded_at DESC`

---

### 3. `generated_pojos`
**Purpose:** Tracks generated POJO classes from XSD schemas

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| schema_id | UUID | PK, FK → schema_metadata | Schema reference |
| pojo_class_name | VARCHAR(500) | PK | Fully qualified class name |
| generated_at | TIMESTAMP | NOT NULL | Generation timestamp |
| has_xml_root_element | BOOLEAN | DEFAULT FALSE | Has @XmlRootElement |

**Composite Primary Key:** `(schema_id, pojo_class_name)`

---

### 4. `service_definition`
**Purpose:** Deployed service definitions and runtime metrics

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Service unique identifier |
| service_name | VARCHAR(255) | UNIQUE, NOT NULL | Service name |
| version | VARCHAR(50) | NOT NULL | Service version |
| schema_id | UUID | NOT NULL, FK → schema_metadata | Schema reference |
| status | VARCHAR(50) | NOT NULL | Deployment status |
| deployed_at | TIMESTAMP | | Deployment timestamp |
| updated_at | TIMESTAMP | | Last update timestamp |
| undeployed_at | TIMESTAMP | | Undeployment timestamp |
| last_health_check | TIMESTAMP | | Last health check |
| deployed_by | VARCHAR(255) | | Deployer username |
| wsdl_content | TEXT | | Generated WSDL document |
| service_version | VARCHAR(50) | | Service version |
| total_requests | BIGINT | DEFAULT 0 | Total request counter |
| successful_requests | BIGINT | DEFAULT 0 | Success counter |
| failed_requests | BIGINT | DEFAULT 0 | Failure counter |
| average_response_time | DOUBLE PRECISION | | Avg response time (ms) |
| last_request_at | TIMESTAMP | | Last request timestamp |
| deployment_errors | TEXT | | Deployment error messages |

**Status Values:**
- `DEPLOYING` - Deployment in progress
- `DEPLOYED` - Successfully deployed and active
- `DEPLOYMENT_FAILED` - Deployment failed
- `UNDEPLOYING` - Undeployment in progress
- `UNDEPLOYED` - Successfully undeployed
- `FAILED` - General failure

**Indexes:**
- `idx_service_name_svc` on `service_name`
- `idx_service_schema_id` on `schema_id`
- `idx_status_svc` on `status`
- `idx_service_deployed_at` on `deployed_at DESC`

**Triggers:**
- `trg_service_definition_updated` - Auto-update `updated_at`

---

### 5. `endpoint_mapping`
**Purpose:** REST and SOAP endpoint mappings

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Endpoint unique identifier |
| service_definition_id | UUID | NOT NULL, FK → service_definition | Service reference |
| endpoint_type | VARCHAR(20) | NOT NULL | REST or SOAP |
| path | VARCHAR(500) | NOT NULL | Endpoint path |
| http_method | VARCHAR(10) | | HTTP method (REST only) |
| operation_name | VARCHAR(255) | | Operation name (SOAP) |
| description | VARCHAR(1000) | | Endpoint description |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE | Active status |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| request_count | BIGINT | DEFAULT 0 | Request counter |
| last_accessed_at | TIMESTAMP | | Last access timestamp |

**Endpoint Types:**
- `REST` - RESTful JSON/XML endpoint
- `SOAP` - SOAP web service endpoint

**HTTP Methods:** (REST only)
- `GET`, `POST`, `PUT`, `DELETE`, `PATCH`

**Indexes:**
- `idx_endpoint_service_def_id` on `service_definition_id`
- `idx_endpoint_type` on `endpoint_type`
- `idx_endpoint_path` on `path`
- `idx_endpoint_active` on `active`

---

## Audit & Monitoring Tables

### 6. `audit_log`
**Purpose:** Comprehensive audit trail

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Log entry identifier |
| timestamp | TIMESTAMP | NOT NULL | Event timestamp |
| username | VARCHAR(255) | NOT NULL | User who performed action |
| action | VARCHAR(100) | NOT NULL | Action type |
| service_name | VARCHAR(255) | | Affected service |
| status | VARCHAR(50) | | Operation status |
| details | VARCHAR(2000) | | Additional details |
| ip_address | VARCHAR(50) | | Client IP address |
| user_agent | VARCHAR(500) | | Client user agent |
| error_message | TEXT | | Error details |
| request_id | UUID | | Request correlation ID |
| duration_ms | BIGINT | | Operation duration |

**Action Types:**
- `SCHEMA_UPLOAD`, `SCHEMA_DELETE`, `SCHEMA_UPDATE`
- `SERVICE_DEPLOY`, `SERVICE_UNDEPLOY`, `SERVICE_REDEPLOY`
- `USER_LOGIN`, `USER_LOGOUT`, `USER_CREATE`, `USER_UPDATE`, `USER_DELETE`
- `ENDPOINT_ACCESS`, `CONFIG_CHANGE`

**Status Values:**
- `SUCCESS` - Operation succeeded
- `FAILED` - Operation failed
- `PARTIAL` - Partial success

**Indexes:**
- `idx_audit_timestamp` on `timestamp DESC`
- `idx_audit_username` on `username`
- `idx_audit_action` on `action`
- `idx_audit_service_name` on `service_name`
- `idx_audit_status` on `status`

---

### 7. `service_metrics`
**Purpose:** Time-series metrics for deployed services

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Metric entry identifier |
| service_definition_id | UUID | NOT NULL, FK → service_definition | Service reference |
| timestamp | TIMESTAMP | NOT NULL | Metric timestamp |
| metric_type | VARCHAR(50) | NOT NULL | Metric type |
| metric_value | DOUBLE PRECISION | NOT NULL | Metric value |
| tags | JSONB | | Additional metadata |
| aggregation_window | VARCHAR(20) | | Time window |

**Metric Types:**
- `REQUEST_COUNT` - Number of requests
- `RESPONSE_TIME` - Response time in ms
- `ERROR_RATE` - Error percentage
- `THROUGHPUT` - Requests per second
- `CPU_USAGE` - CPU usage percentage
- `MEMORY_USAGE` - Memory usage in MB

**Aggregation Windows:**
- `1m`, `5m`, `15m`, `1h`, `1d`

**Indexes:**
- `idx_metrics_service_id` on `service_definition_id`
- `idx_metrics_timestamp` on `timestamp DESC`
- `idx_metrics_type` on `metric_type`

---

## Supporting Tables

### 8. `rate_limit_buckets`
**Purpose:** Token bucket rate limiting

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Bucket identifier |
| identifier | VARCHAR(255) | NOT NULL | User/Service/IP identifier |
| bucket_type | VARCHAR(50) | NOT NULL | Bucket type |
| tokens_remaining | INTEGER | NOT NULL | Available tokens |
| max_tokens | INTEGER | NOT NULL | Maximum tokens |
| refill_rate | INTEGER | NOT NULL | Tokens per second |
| last_refill_at | TIMESTAMP | NOT NULL | Last refill timestamp |
| window_reset_at | TIMESTAMP | NOT NULL | Window reset time |

**Bucket Types:**
- `USER` - Per-user rate limit
- `SERVICE` - Per-service rate limit
- `IP` - Per-IP address rate limit
- `GLOBAL` - Global platform rate limit

**Unique Constraint:** `(identifier, bucket_type)`

**Indexes:**
- `idx_rate_limit_identifier` on `identifier`
- `idx_rate_limit_type` on `bucket_type`

---

### 9. `configuration`
**Purpose:** Platform configuration key-value store

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| key | VARCHAR(255) | PK | Configuration key |
| value | TEXT | NOT NULL | Configuration value |
| value_type | VARCHAR(50) | NOT NULL | Data type |
| description | VARCHAR(1000) | | Key description |
| category | VARCHAR(100) | | Configuration category |
| is_sensitive | BOOLEAN | DEFAULT FALSE | Contains secrets |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | | Last update timestamp |
| updated_by | VARCHAR(255) | | Last updater |

**Value Types:**
- `STRING`, `INTEGER`, `BOOLEAN`, `JSON`

**Categories:**
- `XSD_PROCESSING`, `CODE_GENERATION`, `CLASSLOADER`
- `SECURITY`, `RATE_LIMITING`, `METRICS`, `AUDIT`
- `ENDPOINTS`, `MONITORING`, `LOGGING`, `MAINTENANCE`
- `SCHEMA_MANAGEMENT`

---

### 10. `schema_version`
**Purpose:** Database schema version tracking

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| version | VARCHAR(20) | PK | Schema version |
| applied_at | TIMESTAMP | NOT NULL | Application timestamp |
| description | VARCHAR(500) | | Version description |

---

## Database Views

### `v_active_services`
**Purpose:** Quick view of deployed services with metrics

```sql
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
    (SELECT COUNT(*) FROM endpoint_mapping em
     WHERE em.service_definition_id = sd.id AND em.active = true) as active_endpoints
FROM service_definition sd
JOIN schema_metadata sm ON sd.schema_id = sm.id
WHERE sd.status = 'DEPLOYED';
```

### `v_schema_summary`
**Purpose:** Schema overview with deployment status

```sql
SELECT
    sm.id,
    sm.service_name,
    sm.version,
    sm.status,
    sm.uploaded_at,
    sm.uploaded_by,
    (SELECT COUNT(*) FROM generated_pojos gp WHERE gp.schema_id = sm.id) as pojo_count,
    CASE
        WHEN EXISTS (SELECT 1 FROM service_definition sd
                    WHERE sd.schema_id = sm.id AND sd.status = 'DEPLOYED')
        THEN 'DEPLOYED'
        ELSE 'NOT_DEPLOYED'
    END as deployment_status
FROM schema_metadata sm;
```

### `v_recent_audit`
**Purpose:** Most recent 100 audit log entries

```sql
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
```

---

## Database Functions

### `calculate_success_rate(p_service_id UUID)`
**Purpose:** Calculate success rate percentage for a service

```sql
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
```

**Usage:**
```sql
SELECT service_name, calculate_success_rate(id) as success_rate
FROM service_definition
WHERE status = 'DEPLOYED';
```

---

## Common Queries

### Get all deployed services with metrics
```sql
SELECT * FROM v_active_services
ORDER BY total_requests DESC;
```

### Get schemas awaiting deployment
```sql
SELECT * FROM v_schema_summary
WHERE status = 'ACTIVE' AND deployment_status = 'NOT_DEPLOYED';
```

### Get user activity for last 24 hours
```sql
SELECT username, action, COUNT(*) as action_count
FROM audit_log
WHERE timestamp > NOW() - INTERVAL '24 hours'
GROUP BY username, action
ORDER BY action_count DESC;
```

### Get service performance metrics
```sql
SELECT
    service_name,
    total_requests,
    successful_requests,
    failed_requests,
    ROUND((successful_requests::NUMERIC / NULLIF(total_requests, 0) * 100), 2) as success_rate,
    ROUND(average_response_time, 2) as avg_response_ms
FROM service_definition
WHERE status = 'DEPLOYED'
ORDER BY total_requests DESC;
```

### Get rate limit status for user
```sql
SELECT
    identifier,
    bucket_type,
    tokens_remaining,
    max_tokens,
    ROUND((tokens_remaining::NUMERIC / max_tokens * 100), 2) as capacity_percent
FROM rate_limit_buckets
WHERE identifier = 'admin' AND bucket_type = 'USER';
```

---

## Maintenance

### Cleanup old metrics (keep last 30 days)
```sql
DELETE FROM service_metrics
WHERE timestamp < NOW() - INTERVAL '30 days';
```

### Cleanup old audit logs (keep last 90 days)
```sql
DELETE FROM audit_log
WHERE timestamp < NOW() - INTERVAL '90 days';
```

### Vacuum and analyze
```sql
VACUUM ANALYZE schema_metadata;
VACUUM ANALYZE service_definition;
VACUUM ANALYZE endpoint_mapping;
VACUUM ANALYZE audit_log;
VACUUM ANALYZE service_metrics;
```

---

## Migration Notes

### From JPA to R2DBC

When migrating to R2DBC:

1. **Keep existing tables** - Schema is compatible
2. **Update Spring configuration**:
   ```yaml
   spring:
     r2dbc:
       url: r2dbc:postgresql://localhost:5432/xsdplatform
   ```
3. **Replace JPA repositories** with R2DBC repositories
4. **Convert blocking code** to reactive (Mono/Flux)
5. **Test incrementally** - migrate module by module

---

## Security Considerations

1. **Sensitive Data:**
   - Passwords are BCrypt hashed (strength 10)
   - JWT secrets stored in `configuration` table with `is_sensitive = true`
   - Never log sensitive configuration values

2. **Default Credentials:**
   - Change default user passwords immediately in production
   - Update JWT secret in configuration
   - Enable SSL/TLS for database connections

3. **SQL Injection Prevention:**
   - Always use parameterized queries
   - JPA/R2DBC handle parameterization automatically
   - Never concatenate user input into SQL

4. **Access Control:**
   - Use database roles and permissions
   - Grant minimal privileges to application user
   - Restrict direct database access in production

---

## Performance Tuning

### Connection Pool Settings

**Development:**
```yaml
hikari:
  maximum-pool-size: 10
  minimum-idle: 5
```

**Production:**
```yaml
hikari:
  maximum-pool-size: 50
  minimum-idle: 20
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

### Index Maintenance

Monitor index usage:
```sql
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;
```

Drop unused indexes:
```sql
-- Identify indexes with zero scans
SELECT schemaname, tablename, indexname
FROM pg_stat_user_indexes
WHERE idx_scan = 0 AND indexname NOT LIKE 'pg_%';
```

---

## Backup & Recovery

### Backup Commands

**Full database backup:**
```bash
pg_dump -U postgres -h localhost -d xsdplatform > backup_$(date +%Y%m%d_%H%M%S).sql
```

**Schema only:**
```bash
pg_dump -U postgres -h localhost -d xsdplatform --schema-only > schema_backup.sql
```

**Data only:**
```bash
pg_dump -U postgres -h localhost -d xsdplatform --data-only > data_backup.sql
```

### Restore Commands

**Full restore:**
```bash
psql -U postgres -h localhost -d xsdplatform < backup_20251116_120000.sql
```

**Selective restore:**
```bash
pg_restore -U postgres -h localhost -d xsdplatform -t users backup.dump
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-16 | Initial schema creation |

---

## References

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)
- [Project Reactor](https://projectreactor.io/docs/core/release/reference/)

---

**Document maintained by:** Development Team
**Last reviewed:** 2025-11-16

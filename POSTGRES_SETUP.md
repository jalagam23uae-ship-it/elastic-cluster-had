# PostgreSQL Database Setup Guide

This guide will help you set up PostgreSQL for the Dynamic XSD Service Platform.

## Table of Contents

- [Quick Start with Docker](#quick-start-with-docker)
- [Manual PostgreSQL Installation](#manual-postgresql-installation)
- [Configuration](#configuration)
- [Database Management](#database-management)
- [Troubleshooting](#troubleshooting)

## Quick Start with Docker

The easiest way to get started is using Docker Compose.

### Prerequisites

- Docker installed ([Download Docker](https://docs.docker.com/get-docker/))
- Docker Compose installed ([Install Docker Compose](https://docs.docker.com/compose/install/))

### Step 1: Run the Setup Script

```bash
# Make the script executable (if not already)
chmod +x setup-database.sh

# Run the setup script
./setup-database.sh
```

This script will:
- Check if Docker is installed
- Create a `.env` file from `.env.example`
- Start PostgreSQL container
- Initialize the database
- Start pgAdmin (web-based database management)

### Step 2: Verify PostgreSQL is Running

```bash
# Check container status
docker ps | grep postgres

# Check logs
docker-compose logs postgres

# Test connection
docker exec -it xsd-platform-postgres psql -U postgres -d xsdplatform
```

### Step 3: Run the Application

```bash
cd backend
mvn spring-boot:run
```

The application will automatically connect to PostgreSQL!

## Manual Docker Compose Setup

If you prefer to run commands manually:

```bash
# Start PostgreSQL
docker-compose up -d postgres

# View logs
docker-compose logs -f postgres

# Stop PostgreSQL
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v
```

## Manual PostgreSQL Installation

### On Ubuntu/Debian

```bash
# Update package list
sudo apt update

# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Start PostgreSQL service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Switch to postgres user
sudo -u postgres psql

# Create database and user
CREATE DATABASE xsdplatform;
CREATE USER xsduser WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE xsdplatform TO xsduser;
\q
```

### On macOS

```bash
# Install using Homebrew
brew install postgresql@15

# Start PostgreSQL
brew services start postgresql@15

# Create database
createdb xsdplatform

# Connect to database
psql xsdplatform
```

### On Windows

1. Download PostgreSQL installer from [postgresql.org](https://www.postgresql.org/download/windows/)
2. Run installer and follow the wizard
3. Remember the password you set for the postgres user
4. Use pgAdmin (included with PostgreSQL) or command line to create database:

```sql
CREATE DATABASE xsdplatform;
```

## Configuration

### Environment Variables

Create a `.env` file in the root directory:

```bash
# Copy from example
cp .env.example .env

# Edit the file
nano .env
```

Update the values:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=xsdplatform
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Change this in production!
JWT_SECRET=your-very-long-and-secure-secret-key-here

SPRING_PROFILES_ACTIVE=dev
```

### Application Configuration

The application is already configured to use PostgreSQL. See `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:xsdplatform}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
```

### Connection String Format

The JDBC connection URL format is:

```
jdbc:postgresql://[host]:[port]/[database]?[parameters]
```

Example with SSL:

```
jdbc:postgresql://localhost:5432/xsdplatform?ssl=true&sslmode=require
```

## Database Management

### Using pgAdmin (Web UI)

pgAdmin is included in the Docker Compose setup.

**Access pgAdmin:**
- URL: http://localhost:5050
- Email: admin@admin.com
- Password: admin

**Add Server in pgAdmin:**
1. Right-click "Servers" → Create → Server
2. General tab:
   - Name: XSD Platform
3. Connection tab:
   - Host: postgres (or localhost if not using Docker)
   - Port: 5432
   - Database: xsdplatform
   - Username: postgres
   - Password: postgres
4. Click "Save"

### Using Command Line

```bash
# Connect to database (Docker)
docker exec -it xsd-platform-postgres psql -U postgres -d xsdplatform

# Connect to database (local installation)
psql -h localhost -U postgres -d xsdplatform

# List all tables
\dt

# Describe a table
\d schema_metadata

# Run a query
SELECT * FROM schema_metadata;

# Exit
\q
```

### Common SQL Queries

```sql
-- View all uploaded schemas
SELECT service_name, version, status, uploaded_at
FROM schema_metadata
ORDER BY uploaded_at DESC;

-- View all deployed services
SELECT service_name, status, deployed_at
FROM service_definition
WHERE status = 'DEPLOYED';

-- View recent audit logs
SELECT timestamp, username, action, service_name, status
FROM audit_log
ORDER BY timestamp DESC
LIMIT 20;

-- Count schemas by status
SELECT status, COUNT(*) as count
FROM schema_metadata
GROUP BY status;

-- Clean up old failed schemas
DELETE FROM schema_metadata
WHERE status IN ('VALIDATION_FAILED', 'COMPILATION_FAILED')
AND uploaded_at < NOW() - INTERVAL '7 days';
```

### Backup and Restore

#### Backup Database

```bash
# Using Docker
docker exec xsd-platform-postgres pg_dump -U postgres xsdplatform > backup.sql

# Local installation
pg_dump -U postgres xsdplatform > backup.sql

# Backup with compression
pg_dump -U postgres xsdplatform | gzip > backup.sql.gz
```

#### Restore Database

```bash
# Using Docker
docker exec -i xsd-platform-postgres psql -U postgres xsdplatform < backup.sql

# Local installation
psql -U postgres xsdplatform < backup.sql

# Restore from compressed backup
gunzip -c backup.sql.gz | psql -U postgres xsdplatform
```

### Database Migrations

The application uses Hibernate with `ddl-auto: update`, which automatically creates and updates tables.

For production, consider using a migration tool like **Flyway** or **Liquibase**.

#### Adding Flyway (Optional)

1. Add dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

2. Create migration scripts in `src/main/resources/db/migration/`:

```sql
-- V1__initial_schema.sql
CREATE TABLE IF NOT EXISTS schema_metadata (
    id VARCHAR(255) PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL UNIQUE,
    ...
);
```

3. Update `application.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-create tables
  flyway:
    enabled: true
    baseline-on-migrate: true
```

## Troubleshooting

### Connection Refused

**Error:** `Connection to localhost:5432 refused`

**Solutions:**
1. Check if PostgreSQL is running:
   ```bash
   docker ps | grep postgres
   # or
   sudo systemctl status postgresql
   ```

2. Check if port 5432 is accessible:
   ```bash
   netstat -an | grep 5432
   ```

3. Verify firewall settings

### Authentication Failed

**Error:** `FATAL: password authentication failed for user "postgres"`

**Solutions:**
1. Verify credentials in `.env` file
2. Reset password:
   ```bash
   docker exec -it xsd-platform-postgres psql -U postgres
   ALTER USER postgres PASSWORD 'newpassword';
   ```

### Database Does Not Exist

**Error:** `FATAL: database "xsdplatform" does not exist`

**Solutions:**
1. Create database:
   ```bash
   docker exec -it xsd-platform-postgres createdb -U postgres xsdplatform
   ```

2. Or via SQL:
   ```sql
   CREATE DATABASE xsdplatform;
   ```

### Too Many Connections

**Error:** `FATAL: sorry, too many clients already`

**Solutions:**
1. Increase max connections in PostgreSQL config
2. Reduce HikariCP pool size in `application.yml`:
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 5
   ```

### Slow Queries

**Solutions:**
1. Create indexes:
   ```sql
   CREATE INDEX idx_schema_service_name ON schema_metadata(service_name);
   CREATE INDEX idx_schema_status ON schema_metadata(status);
   ```

2. Analyze query performance:
   ```sql
   EXPLAIN ANALYZE SELECT * FROM schema_metadata WHERE status = 'ACTIVE';
   ```

3. Enable query logging in `application.yml`:
   ```yaml
   logging:
     level:
       org.hibernate.SQL: DEBUG
       org.hibernate.type.descriptor.sql.BasicBinder: TRACE
   ```

### Docker Issues

**Container won't start:**

```bash
# View logs
docker-compose logs postgres

# Restart container
docker-compose restart postgres

# Recreate container
docker-compose down
docker-compose up -d postgres
```

**Port 5432 already in use:**

```bash
# Find what's using the port
sudo lsof -i :5432

# Change port in docker-compose.yml
ports:
  - "5433:5432"  # Use port 5433 on host

# Update DB_PORT in .env
DB_PORT=5433
```

## Production Considerations

### Security

1. **Change default passwords:**
   ```properties
   DB_PASSWORD=your-strong-password-here
   JWT_SECRET=your-long-random-secret-key
   ```

2. **Use secrets management:**
   - AWS Secrets Manager
   - HashiCorp Vault
   - Kubernetes Secrets

3. **Enable SSL:**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://...?ssl=true&sslmode=require
   ```

4. **Restrict network access:**
   - Use VPC/private networks
   - Configure firewall rules
   - Use connection pooling limits

### Performance

1. **Connection pooling:**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 10
         connection-timeout: 30000
         idle-timeout: 600000
         max-lifetime: 1800000
   ```

2. **Database indexes:**
   ```sql
   CREATE INDEX CONCURRENTLY idx_schema_uploaded_at ON schema_metadata(uploaded_at DESC);
   CREATE INDEX CONCURRENTLY idx_audit_timestamp ON audit_log(timestamp DESC);
   ```

3. **Query optimization:**
   - Use pagination for large result sets
   - Add appropriate WHERE clauses
   - Use database views for complex queries

### Monitoring

1. **Enable PostgreSQL logging:**
   ```sql
   ALTER SYSTEM SET log_min_duration_statement = 1000;  -- Log queries > 1s
   SELECT pg_reload_conf();
   ```

2. **Monitor connections:**
   ```sql
   SELECT count(*) FROM pg_stat_activity;
   SELECT * FROM pg_stat_activity WHERE state = 'active';
   ```

3. **Check database size:**
   ```sql
   SELECT pg_size_pretty(pg_database_size('xsdplatform'));
   ```

### Backup Strategy

1. **Automated daily backups:**
   ```bash
   # Add to crontab
   0 2 * * * pg_dump -U postgres xsdplatform | gzip > /backups/xsdplatform-$(date +\%Y\%m\%d).sql.gz
   ```

2. **Retention policy:**
   - Keep daily backups for 7 days
   - Keep weekly backups for 4 weeks
   - Keep monthly backups for 12 months

3. **Test restore regularly:**
   ```bash
   # Test restore to a different database
   createdb test_restore
   gunzip -c backup.sql.gz | psql -U postgres test_restore
   ```

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [pgAdmin Documentation](https://www.pgadmin.org/docs/)

## Support

For issues with PostgreSQL setup:
1. Check the troubleshooting section above
2. Review application logs: `backend/logs/application.log`
3. Check PostgreSQL logs: `docker-compose logs postgres`
4. Open an issue on GitHub with error details

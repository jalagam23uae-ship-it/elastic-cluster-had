# Docker Deployment Guide

This guide explains how to run the XSD Service Generation Platform using Docker and Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB of RAM available for Docker
- At least 10GB of disk space

## Quick Start

### 1. Clone and Navigate

```bash
cd elastic-cluster-had
```

### 2. Configure Environment

```bash
# Copy the example environment file
cp .env.example .env

# Edit .env with your configuration
nano .env
```

### 3. Start All Services

```bash
# Development mode (includes pgAdmin)
docker-compose --profile development up -d

# Production mode (includes Nginx and Redis)
docker-compose --profile production up -d

# Or start specific services
docker-compose up -d postgres backend frontend
```

### 4. Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **pgAdmin** (dev only): http://localhost:5050
- **Nginx** (prod only): http://localhost

### 5. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes data)
docker-compose down -v
```

## Service Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Nginx (Production)                 │
│              Reverse Proxy & Load Balancer           │
└────────────┬────────────────────────────┬───────────┘
             │                            │
      ┌──────▼───────┐            ┌──────▼────────┐
      │   Frontend   │            │    Backend    │
      │  React App   │            │  Spring Boot  │
      │   (Port 80)  │            │  (Port 8080)  │
      └──────────────┘            └───────┬───────┘
                                          │
                  ┌───────────────────────┼──────────────┐
                  │                       │              │
          ┌───────▼────────┐    ┌────────▼────┐  ┌─────▼─────┐
          │   PostgreSQL   │    │   Redis     │  │  pgAdmin  │
          │  (Port 5432)   │    │ (Optional)  │  │ (Dev Only)│
          └────────────────┘    └─────────────┘  └───────────┘
```

## Available Services

### Core Services (Always Running)

1. **PostgreSQL** - Database
   - Port: 5432
   - Health check: PostgreSQL ready check
   - Persistent volume: `postgres_data`

2. **Backend** - Spring Boot Application
   - Port: 8080
   - Health check: `/actuator/health`
   - Volumes:
     - `xsd_storage` - XSD file storage
     - `temp_files` - Temporary compilation files
     - `backend_logs` - Application logs

3. **Frontend** - React Application
   - Port: 3000 (or 80 in container)
   - Nginx-based static file serving
   - Health check: HTTP request to root

### Profile-Based Services

#### Development Profile (`--profile development`)

- **pgAdmin** - Database management UI
  - Port: 5050
  - Default login: admin@admin.com / admin

#### Production Profile (`--profile production`)

- **Nginx** - Reverse proxy
  - Ports: 80 (HTTP), 443 (HTTPS)
  - Routes `/api/*` to backend
  - Routes `/*` to frontend

- **Redis** - Caching (optional)
  - Port: 6379
  - Password-protected

## Building from Source

### Build All Services

```bash
# Build without cache
docker-compose build --no-cache

# Build specific service
docker-compose build backend
docker-compose build frontend
```

### Backend Build

The backend Dockerfile performs a multi-stage build:

1. **Build Stage**: Compiles Java code using Maven
2. **Runtime Stage**: Creates minimal JDK image (required for Java Compiler API)

```bash
cd backend
docker build -t xsd-platform-backend .
```

### Frontend Build

The frontend Dockerfile:

1. **Build Stage**: Installs dependencies and builds React app
2. **Runtime Stage**: Serves static files with Nginx

```bash
cd frontend
docker build -t xsd-platform-frontend .
```

## Environment Variables

### Database

```env
DB_NAME=xsdplatform
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_PORT=5432
```

### Backend

```env
BACKEND_PORT=8080
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xmx1g -Xms512m
JWT_SECRET=your-secret-key-min-256-bits
JWT_EXPIRATION=86400000
```

### Frontend

```env
FRONTEND_PORT=3000
VITE_API_URL=http://localhost:8080
```

### Nginx

```env
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443
```

## Volume Management

### List Volumes

```bash
docker volume ls | grep xsd-platform
```

### Backup Database

```bash
# Export database
docker-compose exec postgres pg_dump -U postgres xsdplatform > backup.sql

# Restore database
docker-compose exec -T postgres psql -U postgres xsdplatform < backup.sql
```

### Backup XSD Storage

```bash
# Create backup
docker run --rm -v xsd_storage:/data -v $(pwd):/backup alpine \
    tar czf /backup/xsd-storage-backup.tar.gz -C /data .

# Restore backup
docker run --rm -v xsd_storage:/data -v $(pwd):/backup alpine \
    tar xzf /backup/xsd-storage-backup.tar.gz -C /data
```

## Logs and Debugging

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend

# Last 100 lines
docker-compose logs --tail=100 backend
```

### Access Container Shell

```bash
# Backend
docker-compose exec backend sh

# PostgreSQL
docker-compose exec postgres psql -U postgres xsdplatform

# Frontend
docker-compose exec frontend sh
```

### Health Checks

```bash
# Check all container health
docker-compose ps

# Check backend health
curl http://localhost:8080/actuator/health

# Check database
docker-compose exec postgres pg_isready -U postgres
```

## Performance Tuning

### Backend JVM Options

Adjust in `.env`:

```env
# For 8GB RAM server
JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# For 4GB RAM server
JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC

# For 2GB RAM server (minimal)
JAVA_OPTS=-Xmx1g -Xms512m
```

### Database Connection Pool

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase for high load
      minimum-idle: 5
      connection-timeout: 30000
```

## Production Deployment

### 1. Enable Production Profile

```bash
# In .env
COMPOSE_PROFILES=production
SPRING_PROFILES_ACTIVE=prod
```

### 2. Configure SSL (Nginx)

Place SSL certificates in `nginx/ssl/`:

```bash
nginx/ssl/
├── cert.pem
└── key.pem
```

Uncomment HTTPS server block in `nginx/conf.d/default.conf`.

### 3. Secure Credentials

```bash
# Generate strong JWT secret (256+ bits)
JWT_SECRET=$(openssl rand -base64 64)

# Set strong database password
DB_PASSWORD=$(openssl rand -base64 32)

# Set Redis password
REDIS_PASSWORD=$(openssl rand -base64 32)
```

### 4. Start Services

```bash
docker-compose --profile production up -d
```

### 5. Verify Deployment

```bash
# Check all services are healthy
docker-compose ps

# Check logs
docker-compose logs --tail=50

# Test backend
curl http://localhost/api/v1/dashboard/health

# Test frontend
curl http://localhost
```

## Monitoring

### Container Stats

```bash
# Real-time stats
docker stats

# Specific container
docker stats xsd-platform-backend
```

### Database Monitoring

Connect to pgAdmin (dev profile) or use CLI:

```bash
docker-compose exec postgres psql -U postgres xsdplatform

# Check active connections
SELECT count(*) FROM pg_stat_activity;

# Check database size
SELECT pg_size_pretty(pg_database_size('xsdplatform'));
```

## Troubleshooting

### Backend Won't Start

```bash
# Check logs
docker-compose logs backend

# Common issues:
# 1. Database not ready - wait for postgres health check
# 2. Port 8080 in use - change BACKEND_PORT in .env
# 3. Out of memory - increase JAVA_OPTS Xmx value
```

### Frontend Build Fails

```bash
# Rebuild with no cache
docker-compose build --no-cache frontend

# Check Node.js version in Dockerfile (should be 20+)
```

### Database Connection Issues

```bash
# Test database connectivity
docker-compose exec backend nc -zv postgres 5432

# Check PostgreSQL logs
docker-compose logs postgres

# Verify credentials in .env
```

### Out of Disk Space

```bash
# Clean up unused images
docker image prune -a

# Clean up unused volumes
docker volume prune

# Clean up everything (WARNING: removes all unused resources)
docker system prune -a --volumes
```

## Scaling

### Horizontal Scaling

```bash
# Scale backend to 3 instances
docker-compose up -d --scale backend=3

# Requires load balancer (Nginx)
```

### Vertical Scaling

Increase resources in `.env`:

```env
JAVA_OPTS=-Xmx4g -Xms2g
```

## CI/CD Integration

### GitLab CI Example

```yaml
build:
  script:
    - docker-compose build
    - docker-compose push

deploy:
  script:
    - docker-compose pull
    - docker-compose up -d
```

### GitHub Actions Example

```yaml
- name: Build and Deploy
  run: |
    docker-compose build
    docker-compose up -d
```

## Security Best Practices

1. **Never commit `.env` file** - Use `.env.example` instead
2. **Change default passwords** - Especially for production
3. **Use secrets management** - Docker secrets or external vault
4. **Enable HTTPS** - Configure SSL certificates in Nginx
5. **Regular updates** - Keep base images updated
6. **Limit resources** - Set memory and CPU limits
7. **Network isolation** - Use custom networks
8. **Read-only filesystems** - Where possible

## Support

For issues and questions:
- Check logs: `docker-compose logs`
- GitHub Issues: [Link to repository]
- Documentation: [Link to docs]

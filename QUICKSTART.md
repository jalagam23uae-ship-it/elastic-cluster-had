# Quick Start Guide

Get the XSD Service Generation Platform running in 5 minutes!

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- 4GB RAM
- 10GB disk space

## 🚀 One-Command Start

```bash
make init
```

This will:
1. Copy `.env.example` to `.env`
2. Build all Docker images
3. Start all services
4. Display access URLs

## 📍 Access Points

After starting, access the application at:

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | React UI |
| **Backend API** | http://localhost:8080 | REST API |
| **Swagger** | http://localhost:8080/swagger-ui.html | API Documentation |
| **pgAdmin** | http://localhost:5050 | Database UI (dev mode) |

**Default Credentials:**
- pgAdmin: `admin@admin.com` / `admin`
- Database: `postgres` / `postgres`

## 📖 Common Commands

```bash
# Development
make dev              # Start development environment
make dev-logs         # Follow application logs

# Viewing Logs
make logs             # All services
make backend-logs     # Backend only
make frontend-logs    # Frontend only

# Database
make db-shell         # PostgreSQL CLI
make db-backup        # Backup database
make db-restore       # Restore from backup

# Maintenance
make health           # Check service health
make ps               # Show service status
make clean            # Stop and remove all
make reset            # Complete reset
```

## 🎯 Quick Test

Once services are running, test the platform:

### 1. Check Health

```bash
make health
```

Should show ✓ for all services.

### 2. Access Frontend

Open http://localhost:3000 in your browser.

### 3. Test Backend API

```bash
# Check health
curl http://localhost:8080/actuator/health

# Get dashboard metrics
curl http://localhost:8080/api/v1/dashboard/health
```

### 4. Upload XSD Schema

Use the frontend at http://localhost:3000 or:

```bash
curl -X POST http://localhost:8080/api/v1/schemas/upload \
  -F "file=@your-schema.xsd" \
  -F "serviceName=myservice" \
  -F "version=1.0.0"
```

### 5. Deploy Service

```bash
curl -X POST http://localhost:8080/api/v1/services/deploy/{schemaId}
```

### 6. Access Generated API

```bash
# REST API
curl http://localhost:8080/api/v1/services/myservice/resources

# WSDL
curl http://localhost:8080/ws/myservice?wsdl
```

## 🔧 Configuration

### Change Ports

Edit `.env`:

```env
BACKEND_PORT=8080    # Backend port
FRONTEND_PORT=3000   # Frontend port
DB_PORT=5432         # PostgreSQL port
```

Then restart:

```bash
make restart
```

### Increase Memory

For backend performance, edit `.env`:

```env
JAVA_OPTS=-Xmx2g -Xms1g  # 2GB max heap
```

## 🐛 Troubleshooting

### Services Won't Start

```bash
# Check what's running
docker ps -a

# Check logs
make logs

# Common fixes
make clean          # Clean everything
make init           # Rebuild and restart
```

### Backend Connection Error

```bash
# Wait for PostgreSQL
docker-compose logs postgres

# Verify database is ready
make db-shell
```

### Frontend Can't Connect

Check `.env`:

```env
VITE_API_URL=http://localhost:8080
```

Should match your backend URL.

### Port Already in Use

Change ports in `.env`:

```env
BACKEND_PORT=8081   # Instead of 8080
FRONTEND_PORT=3001  # Instead of 3000
```

## 📚 More Information

- **Full Docker Guide**: [DOCKER.md](DOCKER.md)
- **Production Deployment**: [DOCKER.md#production-deployment](DOCKER.md#production-deployment)
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Architecture**: See project README

## 🎓 Next Steps

1. **Upload a Schema**: Use the frontend to upload an XSD file
2. **Deploy Service**: Click "Deploy" to generate APIs
3. **Test Generated API**: Use the REST or SOAP endpoints
4. **Monitor**: Check the dashboard for metrics

## 🆘 Need Help?

```bash
# View all available commands
make help

# Check service status
make ps

# View real-time logs
make logs

# Check health status
make health
```

## 🔄 Daily Workflow

```bash
# Morning - Start services
make dev

# Work on code...

# Evening - Stop services
make down

# Weekly - Clean up
make clean
```

## 🚨 Emergency Reset

If everything breaks:

```bash
make reset
```

This will:
1. Stop all containers
2. Remove all volumes
3. Rebuild from scratch
4. Start fresh

---

**Ready to go? Run `make init` and start building!** 🚀

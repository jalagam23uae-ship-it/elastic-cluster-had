# Quick Start Guide

Get up and running with the Dynamic XSD Service Platform in 5 minutes!

## ⚡ Fast Setup (3 Steps)

### Step 1: Start PostgreSQL

```bash
# Navigate to project directory
cd elastic-cluster-had

# Start PostgreSQL using Docker
./setup-database.sh
```

Wait for the script to complete. You'll see:
```
✓ PostgreSQL is ready!
PostgreSQL Setup Complete!
```

### Step 2: Run the Application

```bash
# Navigate to backend directory
cd backend

# Run with Maven
mvn spring-boot:run
```

Wait for the application to start. You'll see:
```
Started DynamicXsdServicePlatformApplication in X.XX seconds
```

### Step 3: Upload a Sample XSD

Open another terminal:

```bash
# Navigate back to project root
cd elastic-cluster-had

# Upload customer service XSD
curl -X POST http://localhost:8080/api/v1/schema/upload \
  -F "file=@sample-xsd/customer.xsd" \
  -F "serviceName=customer-service" \
  -F "version=1.0" \
  -F "description=Customer management service"
```

**Success!** You should see a response like:

```json
{
  "id": "abc-123-xyz",
  "serviceName": "customer-service",
  "version": "1.0",
  "namespace": "http://example.com/customer",
  "status": "ACTIVE",
  "uploadedAt": "2025-11-16T10:00:00",
  "message": "Schema uploaded and processed successfully"
}
```

## 🎉 What Just Happened?

The platform just:

1. ✅ Validated your XSD file (security + syntax)
2. ✅ Generated Java POJOs with JAXB and Jackson annotations
3. ✅ Compiled the Java sources at runtime
4. ✅ Loaded the classes using custom ClassLoader
5. ✅ Saved metadata to PostgreSQL database

Your schema is now **ACTIVE** and ready for service deployment!

## 🔍 Explore the Platform

### 1. Swagger UI - Interactive API Documentation

Open in browser: **http://localhost:8080/swagger-ui.html**

Try these operations:
- `GET /api/v1/schema/list` - View all uploaded schemas
- `GET /api/v1/schema/customer-service` - View details
- `DELETE /api/v1/schema/customer-service` - Delete schema

### 2. pgAdmin - Database Management

Open in browser: **http://localhost:5050**

Login credentials:
- Email: admin@admin.com
- Password: admin

Add server connection:
- Host: postgres
- Port: 5432
- Database: xsdplatform
- Username: postgres
- Password: postgres

Browse tables:
- `schema_metadata` - Uploaded schemas
- `audit_log` - All operations

### 3. Actuator Endpoints - Monitoring

```bash
# Health check
curl http://localhost:8080/actuator/health

# Application metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## 📝 Try More Examples

### Upload Product Service

```bash
curl -X POST http://localhost:8080/api/v1/schema/upload \
  -F "file=@sample-xsd/product.xsd" \
  -F "serviceName=product-service" \
  -F "version=1.0" \
  -F "description=Product catalog service"
```

### List All Schemas

```bash
curl http://localhost:8080/api/v1/schema/list | jq
```

### Get Schema Details

```bash
curl http://localhost:8080/api/v1/schema/customer-service | jq
```

### View Generated POJOs

Check the generated Java files:

```bash
ls -la temp/compile/customer-service/sources/
```

### View Compiled Classes

```bash
ls -la temp/classes/customer-service/
```

## 🐛 Troubleshooting

### PostgreSQL Not Starting?

```bash
# Check if Docker is running
docker ps

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Application Won't Start?

```bash
# Check if PostgreSQL is ready
docker exec xsd-platform-postgres pg_isready -U postgres

# Check if port 8080 is available
lsof -i :8080

# View application logs
tail -f backend/logs/application.log
```

### Connection Refused?

Make sure PostgreSQL is running and accessible:

```bash
# Test connection
docker exec -it xsd-platform-postgres psql -U postgres -d xsdplatform -c "SELECT 1"
```

If you see "1", PostgreSQL is working!

### Build Fails?

Verify you have JDK 21 (not JRE):

```bash
java -version    # Should show JDK 21
javac -version   # Should work (proves it's JDK)
```

## 🎯 Next Steps

1. **Explore the Code**
   - Check out `backend/src/main/java/com/dynamic/xsd/`
   - Read the service implementations
   - Understand the architecture

2. **Upload Your Own XSD**
   - Create your own XSD file
   - Upload it using the API
   - See it get processed automatically

3. **Check the Database**
   - Use pgAdmin to view stored data
   - Run SQL queries on metadata tables
   - Understand the data model

4. **Review Documentation**
   - Read [README.md](README.md) for full documentation
   - Check [POSTGRES_SETUP.md](POSTGRES_SETUP.md) for database details
   - Explore API documentation in Swagger UI

## 💡 Tips

- Use Swagger UI for easy API testing
- Check application logs for debugging: `backend/logs/application.log`
- Use pgAdmin to view database contents
- The sample XSD files are great for testing
- All environment variables can be customized in `.env`

## 🚀 You're All Set!

You now have a fully functional Dynamic XSD Service Platform running locally!

Need help? Check:
- [Full README](README.md)
- [PostgreSQL Setup Guide](POSTGRES_SETUP.md)
- [Troubleshooting Section](README.md#troubleshooting)

Happy coding! 🎉

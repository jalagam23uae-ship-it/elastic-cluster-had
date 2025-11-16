# Dynamic XSD Service Generation Platform

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)
![React](https://img.shields.io/badge/React-18.3.1-61dafb.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)

A comprehensive platform that **automatically generates and deploys REST and SOAP web services** from XSD (XML Schema Definition) files at runtime.

## 🌟 Features

### Core Capabilities
- **📤 XSD Schema Upload** - Upload XSD files via drag-and-drop interface
- **✅ Schema Validation** - Automatic XSD schema validation and parsing
- **🔨 POJO Generation** - Generate Java POJOs with JAXB/Jackson annotations
- **⚡ Runtime Compilation** - Compile generated code on-the-fly using JavaCompiler API
- **🚀 Dynamic Deployment** - Deploy services without restarting the application
- **🔄 REST & SOAP** - Generate both REST (JSON/XML) and SOAP endpoints
- **📄 WSDL Generation** - Automatic WSDL document generation for SOAP services
- **📊 Dashboard** - Real-time monitoring and statistics
- **🔍 Service Catalog** - Browse and discover all available services

### Advanced Features
- **🛡️ Rate Limiting** - Token bucket algorithm for API throttling
- **📝 Audit Logging** - Comprehensive audit trail for all operations
- **📈 Metrics Collection** - Time-series metrics for monitoring
- **🔐 Security** - Spring Security with JWT (ready for integration)
- **🎯 Service Management** - Deploy, undeploy, and manage services
- **🔎 Search & Discovery** - Search services by name and description
- **📦 Endpoint Management** - View and manage all generated endpoints

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React)                        │
│  - File Upload (Drag & Drop)                                │
│  - Dashboard & Analytics                                    │
│  - Service Management                                       │
│  - Schema Management                                        │
└────────────────┬────────────────────────────────────────────┘
                 │ HTTP/REST
                 ▼
┌─────────────────────────────────────────────────────────────┐
│                  Backend (Spring Boot)                      │
│  ┌──────────────┬──────────────┬─────────────────────────┐ │
│  │   Upload     │  Validation  │    Code Generation      │ │
│  │   Service    │   Service    │      Service            │ │
│  └──────────────┴──────────────┴─────────────────────────┘ │
│  ┌──────────────┬──────────────┬─────────────────────────┐ │
│  │ Compilation  │ Deployment   │  Dynamic Endpoint       │ │
│  │   Service    │   Service    │    Registration         │ │
│  └──────────────┴──────────────┴─────────────────────────┘ │
│  ┌──────────────┬──────────────┬─────────────────────────┐ │
│  │ Rate Limit   │ Audit Log    │   Metrics Collection    │ │
│  │   Service    │   Service    │      Service            │ │
│  └──────────────┴──────────────┴─────────────────────────┘ │
└────────────────┬────────────────────────────────────────────┘
                 │ JDBC
                 ▼
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL Database                      │
│  - Schema Metadata                                          │
│  - Service Definitions                                      │
│  - Endpoint Mappings                                        │
│  - Audit Logs & Metrics                                     │
│  - Rate Limit Buckets                                       │
└─────────────────────────────────────────────────────────────┘
```

### Request Flow

```
1. User uploads XSD → Frontend
2. Frontend sends file → Backend /api/v1/schemas/upload
3. Backend validates XSD → XSDValidationService
4. Generate POJOs → XSDCodeGenerationService (JAXB xjc)
5. Compile POJOs → DynamicCodeCompilationService (JavaCompiler)
6. Store metadata → PostgreSQL
7. User deploys schema → /api/v1/services/deploy/{schemaId}
8. Generate endpoints → ServiceDeploymentService
9. Register endpoints → DynamicEndpointRegistrationService
10. Service available → /api/dynamic/{service-name}/**
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Node.js 18** or higher
- **PostgreSQL 15** or higher
- **Maven 3.8** or higher
- **Git**

### 1. Clone Repository

```bash
git clone https://github.com/your-org/elastic-cluster-had.git
cd elastic-cluster-had
```

### 2. Database Setup

```bash
# Create PostgreSQL database
createdb xsd_platform

# Or using psql
psql -U postgres
CREATE DATABASE xsd_platform;
CREATE USER xsd_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE xsd_platform TO xsd_user;
\q
```

### 3. Backend Setup

```bash
cd backend

# Configure application.yml (update database credentials)
nano src/main/resources/application.yml

# Build and run
mvn clean install
mvn spring-boot:run
```

Backend will start on **http://localhost:8080**

### 4. Frontend Setup

```bash
cd ../frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend will start on **http://localhost:5173**

### 5. Open Application

Open your browser and navigate to:
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

---

## 📖 Usage Guide

### Upload XSD Schema

1. Navigate to **Schema Management** page
2. Click **"Upload XSD"** button
3. Drag and drop your XSD file or click to browse
4. Enter service name (e.g., `customer-service`)
5. Optionally enter version and description
6. Check **"Auto Deploy"** to deploy immediately
7. Click **"Upload"**

**Result:** Schema is validated, POJOs generated, code compiled, and stored

### Deploy Service

1. Navigate to **Services** page
2. Click **"Deploy Service"**
3. Select a schema from the dropdown
4. Choose endpoint types:
   - ✓ Enable REST endpoints (JSON/XML)
   - ✓ Enable SOAP endpoints (WSDL)
5. Click **"Deploy"**

**Result:** Service is deployed with endpoints registered at runtime

### Access Generated Endpoints

#### REST Endpoints (JSON/XML)
```bash
# GET - Retrieve all entities
curl http://localhost:8080/api/dynamic/customer-service/Customer

# GET - Retrieve by ID
curl http://localhost:8080/api/dynamic/customer-service/Customer/1

# POST - Create entity (JSON)
curl -X POST http://localhost:8080/api/dynamic/customer-service/Customer \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'

# POST - Create entity (XML)
curl -X POST http://localhost:8080/api/dynamic/customer-service/Customer \
  -H "Content-Type: application/xml" \
  -d '<Customer><name>John Doe</name><email>john@example.com</email></Customer>'

# PUT - Update entity
curl -X PUT http://localhost:8080/api/dynamic/customer-service/Customer/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Doe","email":"jane@example.com"}'

# DELETE - Delete entity
curl -X DELETE http://localhost:8080/api/dynamic/customer-service/Customer/1
```

#### SOAP Endpoints (WSDL)
```bash
# Get WSDL
curl http://localhost:8080/api/v1/services/wsdl/customer-service?wsdl

# SOAP Request Example
curl -X POST http://localhost:8080/soap/customer-service \
  -H "Content-Type: text/xml" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
        <soapenv:Body>
          <ns:getCustomer xmlns:ns="http://example.com/customer">
            <id>1</id>
          </ns:getCustomer>
        </soapenv:Body>
      </soapenv:Envelope>'
```

### View Dashboard

Navigate to **Dashboard** to see:
- Total services, schemas, and endpoints
- Total requests and average response time
- Recent services
- System status

---

## 📁 Project Structure

```
elastic-cluster-had/
├── backend/                          # Spring Boot Backend
│   ├── src/main/java/com/dynamic/xsd/
│   │   ├── config/                   # Configuration classes
│   │   │   ├── AsyncConfig.java      # Async/Scheduling config
│   │   │   ├── RateLimitInterceptor.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebMvcConfig.java
│   │   ├── controller/               # REST Controllers
│   │   │   ├── CatalogController.java       # Service catalog
│   │   │   ├── DynamicServiceController.java # Dynamic endpoints
│   │   │   ├── ManagementController.java    # Admin management
│   │   │   ├── SchemaController.java        # Schema management
│   │   │   └── ServiceController.java       # Service deployment
│   │   ├── domain/
│   │   │   ├── entity/               # JPA Entities
│   │   │   │   ├── AuditLog.java
│   │   │   │   ├── EndpointMapping.java
│   │   │   │   ├── RateLimitBucket.java
│   │   │   │   ├── SchemaMetadata.java
│   │   │   │   ├── ServiceDefinition.java
│   │   │   │   ├── ServiceMetric.java
│   │   │   │   └── User.java
│   │   │   └── enums/                # Enumerations
│   │   ├── dto/                      # Data Transfer Objects
│   │   ├── repository/               # Spring Data JPA Repositories
│   │   ├── scheduler/                # Scheduled tasks
│   │   │   └── CleanupScheduler.java
│   │   └── service/                  # Business Logic Services
│   │       ├── AuditLogService.java
│   │       ├── DynamicCodeCompilationService.java
│   │       ├── DynamicEndpointRegistrationService.java
│   │       ├── MetricsCollectionService.java
│   │       ├── RateLimitService.java
│   │       ├── ServiceDeploymentService.java
│   │       ├── XSDCodeGenerationService.java
│   │       └── XSDValidationService.java
│   ├── src/main/resources/
│   │   ├── application.yml           # Main configuration
│   │   ├── schema.sql                # Database schema
│   │   └── data.sql                  # Seed data
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                         # React Frontend
│   ├── src/
│   │   ├── api/                      # API Services
│   │   │   ├── catalogService.ts
│   │   │   ├── client.ts             # Axios client
│   │   │   ├── metricsService.ts
│   │   │   ├── schemaService.ts
│   │   │   └── serviceService.ts
│   │   ├── components/
│   │   │   ├── common/               # Reusable components
│   │   │   │   ├── Button.tsx
│   │   │   │   ├── Card.tsx
│   │   │   │   ├── FileUpload.tsx
│   │   │   │   ├── Modal.tsx
│   │   │   │   └── StatusBadge.tsx
│   │   │   └── layout/               # Layout components
│   │   │       ├── Header.tsx
│   │   │       ├── MainLayout.tsx
│   │   │       └── Sidebar.tsx
│   │   ├── config/
│   │   │   └── api.ts                # API configuration
│   │   ├── pages/                    # Page components
│   │   │   ├── Dashboard.tsx
│   │   │   ├── SchemaManagement.tsx
│   │   │   └── ServicesPage.tsx
│   │   ├── types/                    # TypeScript types
│   │   │   ├── api.ts
│   │   │   ├── metrics.ts
│   │   │   ├── schema.ts
│   │   │   └── service.ts
│   │   ├── App.tsx                   # Root component
│   │   └── main.tsx                  # Entry point
│   ├── package.json
│   └── vite.config.ts
│
├── docker-compose.yml                # Docker orchestration
├── .gitignore
└── README.md                         # This file
```

---

## 🛠️ Technology Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming language |
| Spring Boot | 3.5.0 | Application framework |
| Spring Data JPA | 3.5.0 | Data access layer |
| PostgreSQL | 15+ | Database |
| HikariCP | Latest | Connection pooling |
| JAXB | 4.0.5 | XML binding (POJO generation) |
| Jackson | Latest | JSON serialization |
| Lombok | Latest | Boilerplate reduction |
| Springdoc OpenAPI | Latest | API documentation |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.3.1 | UI framework |
| TypeScript | 5.9.3 | Type safety |
| Vite | 7.2.2 | Build tool |
| TanStack Query | 5.90.9 | Server state management |
| React Router | 7.9.6 | Routing |
| Axios | 1.13.2 | HTTP client |
| Tailwind CSS | 4.1.17 | Styling |
| React Dropzone | Latest | File upload |
| React Hot Toast | Latest | Notifications |
| Lucide React | Latest | Icons |
| date-fns | Latest | Date formatting |

### Infrastructure
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **Nginx** - Reverse proxy (in Docker setup)

---

## 🔧 Configuration

### Backend Configuration (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/xsd_platform
    username: xsd_user
    password: your_password

  jpa:
    hibernate:
      ddl-auto: validate  # Uses schema.sql
    show-sql: true

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

server:
  port: 8080

# Application settings
app:
  xsd:
    upload-dir: ./uploads
    generated-src-dir: ./generated-sources
    compiled-classes-dir: ./compiled-classes
    max-file-size: 5MB

  rate-limit:
    default-max-tokens: 100
    default-refill-rate: 10  # tokens per second

  cleanup:
    audit-retention-days: 90
    metrics-retention-days: 30
```

### Frontend Configuration (`.env`)

```bash
VITE_API_BASE_URL=http://localhost:8080
```

---

## 📊 API Documentation

### Schema Management
- `POST /api/v1/schemas/upload` - Upload XSD schema
- `GET /api/v1/schemas` - List schemas (paginated)
- `GET /api/v1/schemas/{serviceName}` - Get schema details
- `DELETE /api/v1/schemas/{serviceName}` - Delete schema

### Service Deployment
- `POST /api/v1/services/deploy/{schemaId}` - Deploy service
- `POST /api/v1/services/undeploy/{serviceId}` - Undeploy service
- `GET /api/v1/services/status/{serviceId}` - Get deployment status
- `GET /api/v1/services/wsdl/{serviceName}?wsdl` - Get WSDL document

### Catalog & Discovery
- `GET /api/v1/catalog/services` - List all deployed services
- `GET /api/v1/catalog/services/{serviceName}` - Get service details
- `GET /api/v1/catalog/schemas` - List schemas
- `GET /api/v1/catalog/endpoints` - List all endpoints
- `GET /api/v1/catalog/search?query={q}` - Search services

### Management & Monitoring
- `GET /api/v1/management/rate-limit/{username}` - Get rate limit status
- `GET /api/v1/management/audit` - Get audit logs (Admin only)
- `GET /api/v1/management/metrics/{serviceId}` - Get service metrics
- `POST /api/v1/management/cleanup/audit-logs` - Cleanup old audit logs

### Dynamic Endpoints (Generated at Runtime)
- `GET /api/dynamic/{serviceName}/{EntityName}` - Get all entities
- `GET /api/dynamic/{serviceName}/{EntityName}/{id}` - Get entity by ID
- `POST /api/dynamic/{serviceName}/{EntityName}` - Create entity
- `PUT /api/dynamic/{serviceName}/{EntityName}/{id}` - Update entity
- `DELETE /api/dynamic/{serviceName}/{EntityName}/{id}` - Delete entity

**Full API documentation available at:** http://localhost:8080/swagger-ui.html

---

## 🧪 Testing

### Backend Tests

```bash
cd backend
mvn test                    # Run all tests
mvn test -Dtest=SchemaControllerTest  # Run specific test
```

### Frontend Tests

```bash
cd frontend
npm run lint                # TypeScript type checking
npm run build               # Production build test
```

### Integration Testing

```bash
# Start backend
cd backend && mvn spring-boot:run

# In another terminal, test endpoints
curl -X POST http://localhost:8080/api/v1/schemas/upload \
  -F "file=@examples/customer.xsd" \
  -F "serviceName=customer-service" \
  -F "version=1.0"
```

---

## 🐳 Docker Deployment

### Using Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

**Services:**
- **Backend**: http://localhost:8080
- **Frontend**: http://localhost:3000
- **PostgreSQL**: localhost:5432
- **Nginx**: http://localhost (reverse proxy)

---

## 🔐 Security Features

### Rate Limiting
- Token bucket algorithm
- Per-user, per-service, per-IP limits
- Configurable max tokens and refill rates
- Automatic bucket refill
- Rate limit headers in responses

### Audit Logging
- All operations logged with timestamps
- User tracking (username, IP, user agent)
- Request/response correlation
- Success/failure tracking
- Duration tracking
- Async logging for performance

### Authentication (Ready for Integration)
- JWT-based authentication prepared
- Spring Security configured
- User roles (ADMIN, USER, VIEWER)
- Protected endpoints with `@PreAuthorize`

**Note:** Authentication is currently disabled for development. Enable in `SecurityConfig.java`.

---

## 📈 Monitoring & Metrics

### Available Metrics
- Request count per service
- Average response time
- Error rates
- Success rates
- Throughput
- Memory usage
- CPU usage

### Time-Series Data
- Metrics stored with timestamps
- Configurable aggregation windows (1m, 5m, 15m, 1h, 1d)
- Statistical analysis (avg, min, max, percentiles)
- JSONB tags for flexible querying

### Cleanup Jobs
- **Audit logs**: Retained for 90 days (configurable)
- **Metrics**: Retained for 30 days (configurable)
- **Scheduled cleanup**: Daily at 2 AM
- **Health metrics**: Collected every 60 seconds

---

## 🐛 Troubleshooting

### Database Connection Issues
```bash
# Verify PostgreSQL is running
pg_isready

# Check connection
psql -U xsd_user -d xsd_platform

# Reset database
dropdb xsd_platform
createdb xsd_platform
```

### Backend Compilation Issues
```bash
# Clean and rebuild
mvn clean install -DskipTests

# Check Java version
java -version  # Should be 21+
```

### Frontend Build Issues
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install

# Check Node version
node -v  # Should be 18+
```

### CORS Issues
- Ensure frontend URL is in `WebMvcConfig.java` allowed origins
- Default: `http://localhost:3000` and `http://localhost:5173`
- Add custom origins if needed

### File Upload Issues
- Check `app.xsd.max-file-size` in `application.yml`
- Verify upload directory exists and is writable
- Check disk space

---

## 🚀 Deployment to Production

### Backend

```bash
# Build JAR
cd backend
mvn clean package -DskipTests

# Run JAR
java -jar target/xsd-platform-backend-1.0.0.jar \
  --spring.profiles.active=production

# Or with custom config
java -jar target/xsd-platform-backend-1.0.0.jar \
  --spring.config.location=/etc/xsd-platform/application.yml
```

### Frontend

```bash
# Build for production
cd frontend
npm run build

# Output in dist/
# Serve with Nginx, Apache, or any static file server
```

### Environment Variables

```bash
# Backend
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/xsd_platform
export SPRING_DATASOURCE_USERNAME=xsd_user
export SPRING_DATASOURCE_PASSWORD=secure_password
export APP_XSD_UPLOAD_DIR=/var/xsd-platform/uploads

# Frontend
export VITE_API_BASE_URL=https://api.yourcompany.com
```

---

## 📝 Development Workflow

### Adding New Features

1. **Backend:**
   - Create entity in `domain/entity/`
   - Create repository in `repository/`
   - Create service in `service/`
   - Create controller in `controller/`
   - Add tests

2. **Frontend:**
   - Create TypeScript types in `types/`
   - Create API service in `api/`
   - Create component/page in `components/` or `pages/`
   - Add to router in `App.tsx`

### Code Style

**Backend (Java):**
- Follow Google Java Style Guide
- Use Lombok for boilerplate
- Add Javadoc for public methods
- Use meaningful variable names

**Frontend (TypeScript):**
- Use functional components with hooks
- Follow React best practices
- Add TypeScript types for everything
- Use Tailwind for styling

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👥 Team

- **Backend Development**: Spring Boot, PostgreSQL, XSD Processing
- **Frontend Development**: React, TypeScript, Tailwind CSS
- **DevOps**: Docker, Docker Compose, CI/CD

---

## 📞 Support

For issues and questions:
- **GitHub Issues**: https://github.com/your-org/elastic-cluster-had/issues
- **Documentation**: https://docs.yourcompany.com/xsd-platform
- **Email**: support@yourcompany.com

---

## 🗺️ Roadmap

### Phase 1 (Current) ✅
- [x] XSD upload and validation
- [x] POJO generation
- [x] Runtime compilation
- [x] Service deployment
- [x] REST/SOAP endpoint generation
- [x] Dashboard and monitoring
- [x] Rate limiting
- [x] Audit logging

### Phase 2 (Planned)
- [ ] JWT Authentication implementation
- [ ] User management UI
- [ ] Advanced metrics visualization
- [ ] Service versioning
- [ ] API testing interface
- [ ] Export WSDL/OpenAPI specs
- [ ] Bulk operations

### Phase 3 (Future)
- [ ] Multi-tenancy support
- [ ] GraphQL endpoint generation
- [ ] Service mesh integration
- [ ] Advanced caching
- [ ] Real-time notifications
- [ ] Service orchestration
- [ ] AI-powered schema optimization

---

## ⭐ Acknowledgments

- **JAXB** - XML binding and code generation
- **Spring Boot** - Application framework
- **React** - UI library
- **PostgreSQL** - Robust database
- **TanStack Query** - Server state management
- **Tailwind CSS** - Utility-first CSS

---

Made with ❤️ by the XSD Platform Team

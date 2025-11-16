# Dynamic XSD Service Generation Platform

A powerful Spring Boot 3.x platform that dynamically generates REST APIs and SOAP Web Services from uploaded XSD (XML Schema Definition) files. Upload an XSD schema, and the platform automatically creates Java POJOs, compiles them at runtime, and exposes them as fully functional REST and SOAP services—all without restarting the application.

## 🚀 Features

### Core Capabilities
- **Dynamic XSD Processing**: Upload XSD files and automatically generate services
- **POJO Generation**: Uses JAXB XJC API to generate Java classes from XSD schemas
- **Runtime Compilation**: Compiles generated sources using Java Compiler API
- **Isolated Class Loading**: Custom ClassLoaders provide service isolation
- **Dual Protocol Support**: Auto-generates both REST and SOAP endpoints
- **Multi-Format Support**: REST APIs support both JSON and XML
- **WSDL Generation**: Automatic WSDL generation for SOAP services

### Technical Features
- **Java 21** with latest language features
- **Spring Boot 3.3.0** with Spring Web and Spring Web Services
- **JAXB 4.x** for XML binding and code generation
- **Woodstox** for high-performance XML parsing
- **Jackson** for JSON processing
- **H2/PostgreSQL** for metadata storage
- **OpenAPI/Swagger** for API documentation
- **Spring Boot Actuator** for monitoring and metrics

### Security & Validation
- XXE (XML External Entity) attack prevention
- XML bomb attack prevention
- Schema complexity validation
- File size limits
- Input validation with Bean Validation

## 📋 Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [How It Works](#how-it-works)
- [Usage Examples](#usage-examples)
- [Configuration](#configuration)
- [Development](#development)
- [Troubleshooting](#troubleshooting)

## 🏗️ Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Applications                      │
│            (Web UI, Postman, SOAP Client, etc.)             │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Application                    │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │  Schema Mgmt API │      │  Dynamic REST    │            │
│  │  (Upload/Delete) │      │  APIs (JSON/XML) │            │
│  └──────────────────┘      └──────────────────┘            │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │  Service Mgmt    │      │  SOAP Services   │            │
│  │  API             │      │  (WSDL)          │            │
│  └──────────────────┘      └──────────────────┘            │
├─────────────────────────────────────────────────────────────┤
│                   Orchestration Layer                        │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Schema Management Service                   │    │
│  └────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                      Core Services                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ XSD Validator│  │ POJO Generator│  │   Compiler   │     │
│  │  (Woodstox)  │  │  (JAXB XJC)   │  │  (Java API)  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  ClassLoader │  │ REST Generator│  │ SOAP Generator│     │
│  │   Manager    │  │               │  │               │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                   Persistence Layer                          │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐           │
│  │  Schema    │  │  Service   │  │  Audit     │           │
│  │  Metadata  │  │ Definition │  │    Log     │           │
│  └────────────┘  └────────────┘  └────────────┘           │
├─────────────────────────────────────────────────────────────┤
│                  Database (H2/PostgreSQL)                    │
└─────────────────────────────────────────────────────────────┘
```

### Processing Flow

```
Upload XSD
    │
    ▼
XSD Validation (Security + Syntax)
    │
    ▼
Parse XSD & Extract Metadata
    │
    ▼
Generate Java POJOs (JAXB XJC)
    │
    ├─→ Add JAXB Annotations
    └─→ Add Jackson Annotations
    │
    ▼
Compile Java Sources (Runtime)
    │
    ▼
Load Compiled Classes (Custom ClassLoader)
    │
    ▼
Register Service (Ready for Deployment)
    │
    ▼
Deploy Service
    │
    ├─→ Generate REST Endpoints
    │   ├─→ POST /api/dynamic/{service}/{entity}
    │   ├─→ GET  /api/dynamic/{service}/{entity}/{id}
    │   ├─→ PUT  /api/dynamic/{service}/{entity}/{id}
    │   └─→ DELETE /api/dynamic/{service}/{entity}/{id}
    │
    └─→ Generate SOAP Endpoints
        ├─→ SOAP Service at /ws/{service}
        └─→ WSDL at /ws/{service}?wsdl
```

## 📦 Prerequisites

### Required Software
- **JDK 21** (OpenJDK or Oracle JDK)
  - ⚠️ **Important**: Must be JDK, not JRE (Java Compiler API requires JDK)
  - Verify: `java -version` and `javac -version`
- **Maven 3.8+** or **Gradle 8+**
- **Git**

### Optional (for Production)
- **PostgreSQL 14+** (if not using H2)
- **Docker** (for containerized deployment)
- **Node.js 18+** (for frontend development)

### System Requirements
- **Memory**: Minimum 2GB RAM (4GB+ recommended)
- **Disk Space**: 1GB free space
- **OS**: Linux, macOS, or Windows

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd elastic-cluster-had
```

### 2. Build the Backend

```bash
cd backend
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/xsd-service-platform-1.0.0.jar
```

### 4. Verify Installation

The application will start on **http://localhost:8080**

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:xsdplatform`
  - Username: `sa`
  - Password: (leave empty)
- **Actuator Health**: http://localhost:8080/actuator/health

### 5. Upload Your First XSD

Using curl:

```bash
curl -X POST http://localhost:8080/api/v1/schema/upload \
  -F "file=@sample-xsd/customer.xsd" \
  -F "serviceName=customer-service" \
  -F "version=1.0" \
  -F "description=Customer management service"
```

Using Swagger UI:
1. Navigate to http://localhost:8080/swagger-ui.html
2. Find `POST /api/v1/schema/upload`
3. Click "Try it out"
4. Choose a file (use `sample-xsd/customer.xsd`)
5. Fill in parameters
6. Click "Execute"

## 📂 Project Structure

```
elastic-cluster-had/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/dynamic/xsd/
│   │   │   │   ├── config/                 # Configuration classes
│   │   │   │   │   ├── DynamicServiceProperties.java
│   │   │   │   │   └── WebConfig.java
│   │   │   │   ├── controller/             # REST Controllers
│   │   │   │   │   ├── SchemaManagementController.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   ├── domain/
│   │   │   │   │   └── entity/             # JPA Entities
│   │   │   │   │       ├── SchemaMetadata.java
│   │   │   │   │       ├── ServiceDefinition.java
│   │   │   │   │       ├── EndpointMapping.java
│   │   │   │   │       ├── User.java
│   │   │   │   │       └── AuditLog.java
│   │   │   │   ├── dto/                    # Data Transfer Objects
│   │   │   │   │   ├── SchemaUploadRequest.java
│   │   │   │   │   ├── SchemaUploadResponse.java
│   │   │   │   │   └── ...
│   │   │   │   ├── repository/             # JPA Repositories
│   │   │   │   │   ├── SchemaMetadataRepository.java
│   │   │   │   │   └── ...
│   │   │   │   ├── service/                # Business Logic
│   │   │   │   │   ├── XsdValidatorService.java
│   │   │   │   │   ├── PojoGeneratorService.java
│   │   │   │   │   ├── DynamicCompilerService.java
│   │   │   │   │   ├── SchemaManagementService.java
│   │   │   │   │   └── classloader/
│   │   │   │   │       ├── ServiceClassLoader.java
│   │   │   │   │       └── ClassLoaderManager.java
│   │   │   │   └── DynamicXsdServicePlatformApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       └── application-prod.yml
│   │   └── test/
│   └── pom.xml
├── sample-xsd/                             # Sample XSD files
│   ├── customer.xsd
│   └── product.xsd
└── README.md
```

## 📚 API Documentation

### Schema Management APIs

#### 1. Upload XSD Schema

**POST** `/api/v1/schema/upload`

Uploads an XSD file and processes it (validation, POJO generation, compilation).

**Request:**
- Content-Type: `multipart/form-data`
- Parameters:
  - `file` (file, required): XSD file to upload
  - `serviceName` (string, required): Unique service name (lowercase, alphanumeric, hyphens)
  - `version` (string, optional): Version in x.y or x.y.z format (default: 1.0)
  - `description` (string, optional): Service description
  - `namespace` (string, optional): Target namespace (auto-detected if not provided)
  - `autoDeploy` (boolean, optional): Auto-deploy after upload (default: false)

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "customer-service",
  "version": "1.0",
  "namespace": "http://example.com/customer",
  "status": "ACTIVE",
  "uploadedAt": "2025-11-16T10:00:00",
  "validationWarnings": [],
  "message": "Schema uploaded and processed successfully"
}
```

**Status Values:**
- `VALIDATING`: XSD validation in progress
- `VALIDATION_FAILED`: XSD validation failed
- `GENERATING`: POJO generation in progress
- `GENERATION_FAILED`: POJO generation failed
- `COMPILING`: Compilation in progress
- `COMPILATION_FAILED`: Compilation failed
- `ACTIVE`: Successfully processed and ready for deployment
- `DEPRECATED`: Schema marked for deletion

#### 2. Get Schema by Service Name

**GET** `/api/v1/schema/{serviceName}`

Retrieves metadata for a specific schema.

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "customer-service",
  "version": "1.0",
  "namespace": "http://example.com/customer",
  "description": "Customer management service",
  "status": "ACTIVE",
  "uploadedBy": "admin",
  "uploadedAt": "2025-11-16T10:00:00",
  "updatedAt": "2025-11-16T10:01:00",
  "generatedArtifacts": {
    "pojos": [
      "com.generated.customerservice.model.Customer",
      "com.generated.customerservice.model.Address"
    ],
    "restEndpointCount": 5,
    "soapOperationCount": 3
  }
}
```

#### 3. List All Schemas

**GET** `/api/v1/schema/list`

Retrieves a paginated list of all schemas with optional filtering.

**Query Parameters:**
- `status` (string, optional): Filter by status (ACTIVE, FAILED, etc.)
- `search` (string, optional): Search by service name
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20)
- `sort` (string, optional): Sort field and direction (e.g., `uploadedAt,desc`)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "...",
      "serviceName": "customer-service",
      "status": "ACTIVE",
      "uploadedAt": "2025-11-16T10:00:00",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 42,
  "totalPages": 3
}
```

#### 4. Delete Schema

**DELETE** `/api/v1/schema/{serviceName}`

Deletes a schema and all associated resources (compiled classes, classloaders, XSD files).

**Response (204 No Content)**

### Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2025-11-16T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Service name already exists: customer-service",
  "path": "/api/v1/schema/upload",
  "errors": [
    {
      "field": "serviceName",
      "message": "Service name already exists",
      "rejectedValue": "customer-service"
    }
  ],
  "traceId": "abc123xyz"
}
```

**Common Status Codes:**
- `400 Bad Request`: Invalid input or validation failure
- `404 Not Found`: Schema not found
- `409 Conflict`: Service name already exists
- `413 Payload Too Large`: File size exceeds limit
- `500 Internal Server Error`: Unexpected server error

## 🔧 How It Works

### 1. XSD Validation

When you upload an XSD file, the platform:

- **Security Checks**:
  - Prevents XXE (XML External Entity) attacks
  - Prevents XML bomb attacks
  - Validates file size limits

- **Syntax Validation**:
  - Uses javax.xml.validation.SchemaFactory
  - Validates against W3C XML Schema standard

- **Metadata Extraction**:
  - Extracts target namespace
  - Analyzes schema complexity
  - Counts elements, types, and nesting depth

### 2. POJO Generation

Using **JAXB XJC API** (com.sun.tools.xjc):

```java
SchemaCompiler compiler = XJC.createSchemaCompiler();
compiler.setDefaultPackageName("com.generated.{serviceName}.model");
compiler.parseSchema(inputSource);
S2JJAXBModel model = compiler.bind();
JCodeModel codeModel = model.generateCode(null, null);
codeModel.build(outputDirectory);
```

Generated POJOs include:

- **JAXB Annotations**: `@XmlRootElement`, `@XmlElement`, `@XmlAttribute`
- **Jackson Annotations**: `@JsonProperty`, `@JsonRootName` (added via post-processing)
- **Bean Validation**: `@NotNull`, `@Size` (from XSD constraints)
- **Lombok Support**: Can be added for cleaner code

Example generated class:

```java
package com.generated.customerservice.model;

import jakarta.xml.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name = "Customer")
@JsonRootName("Customer")
public class Customer {

    @XmlElement(required = true)
    @JsonProperty("customerId")
    private Long customerId;

    @XmlElement(required = true)
    @JsonProperty("firstName")
    private String firstName;

    // ... getters and setters
}
```

### 3. Runtime Compilation

Using **Java Compiler API** (javax.tools.JavaCompiler):

```java
JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
StandardJavaFileManager fileManager = compiler.getStandardFileManager(...);
JavaCompiler.CompilationTask task = compiler.getTask(...);
boolean success = task.call();
```

**Important**: Requires JDK (not JRE) to access the compiler.

### 4. Dynamic Class Loading

Custom **ServiceClassLoader** extends URLClassLoader:

- **Isolated Class Loading**: Each service has its own classloader
- **Parent-Last Delegation**: Optional for maximum isolation
- **Class Caching**: Improves performance
- **Proper Cleanup**: Prevents memory leaks

```java
ServiceClassLoader classLoader = new ServiceClassLoader(
    serviceName,
    classPath,
    parentClassLoader,
    parentFirst
);
Class<?> clazz = classLoader.loadClass("com.generated.customerservice.model.Customer");
```

### 5. Service Deployment (Future)

The platform will dynamically:

- **REST Endpoints**:
  - Generate `@RestController` classes
  - Register with `RequestMappingHandlerMapping`
  - Support JSON and XML (Content Negotiation)

- **SOAP Services**:
  - Generate `@Endpoint` classes
  - Create WSDL dynamically
  - Register with Spring WS endpoint registry

## 💡 Usage Examples

### Example 1: Upload Customer Service XSD

```bash
curl -X POST http://localhost:8080/api/v1/schema/upload \
  -F "file=@sample-xsd/customer.xsd" \
  -F "serviceName=customer-service" \
  -F "version=1.0" \
  -F "description=Customer management service"
```

**Response:**
```json
{
  "id": "abc123",
  "serviceName": "customer-service",
  "version": "1.0",
  "namespace": "http://example.com/customer",
  "status": "ACTIVE",
  "uploadedAt": "2025-11-16T10:00:00",
  "message": "Schema uploaded and processed successfully"
}
```

### Example 2: List All Schemas

```bash
curl -X GET "http://localhost:8080/api/v1/schema/list?page=0&size=10&status=ACTIVE"
```

### Example 3: Get Schema Details

```bash
curl -X GET "http://localhost:8080/api/v1/schema/customer-service"
```

### Example 4: Delete Schema

```bash
curl -X DELETE "http://localhost:8080/api/v1/schema/customer-service"
```

## ⚙️ Configuration

### Application Properties

Edit `backend/src/main/resources/application.yml`:

```yaml
dynamic-service:
  xsd:
    storage-path: ./xsd-storage          # XSD file storage location
    max-file-size: 5242880               # Max file size in bytes (5MB)
    validation-enabled: true             # Enable XSD validation

  compilation:
    temp-directory: ./temp/compile       # Temporary compilation directory
    output-directory: ./temp/classes     # Compiled class output directory
    keep-sources: true                   # Keep generated source files
    java-version: "21"                   # Java version for compilation

  classloader:
    isolation-enabled: true              # Enable classloader isolation
    parent-first: false                  # Use parent-last delegation
    max-services: 100                    # Maximum number of services

  rest:
    base-path: /api/dynamic              # Base path for dynamic REST APIs
    versioning-enabled: true             # Enable API versioning
    default-page-size: 20                # Default pagination size

  soap:
    base-path: /ws                       # Base path for SOAP services
    wsdl-enabled: true                   # Enable WSDL generation
    soap-version: "1.2"                  # SOAP version (1.1 or 1.2)
```

### Database Configuration

#### H2 (Development)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:xsdplatform
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

#### PostgreSQL (Production)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/xsdplatform
    username: postgres
    password: yourpassword

  jpa:
    hibernate:
      ddl-auto: validate
```

### Profiles

- **dev**: Development profile with debug logging
- **prod**: Production profile with optimized settings

Run with profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Or:

```bash
java -jar -Dspring.profiles.active=prod target/xsd-service-platform-1.0.0.jar
```

## 🛠️ Development

### Building from Source

```bash
cd backend
mvn clean package
```

### Running Tests

```bash
mvn test
```

### Code Style

The project follows standard Java conventions. Use your IDE's formatter with:
- Indentation: 4 spaces
- Line length: 120 characters

### Adding New Features

1. Create feature branch: `git checkout -b feature/your-feature`
2. Implement changes
3. Write tests
4. Commit: `git commit -m "Add: your feature description"`
5. Push: `git push origin feature/your-feature`
6. Create pull request

## 🐛 Troubleshooting

### Issue: "Java Compiler not available"

**Cause**: Running with JRE instead of JDK

**Solution**:
```bash
# Check Java installation
java -version
javac -version  # Should work

# Set JAVA_HOME to JDK
export JAVA_HOME=/path/to/jdk-21
```

### Issue: "Class loading failed"

**Cause**: Compilation errors or missing dependencies

**Solution**:
- Check compilation errors in logs
- Verify XSD is valid
- Check file permissions on temp directories

### Issue: "File size exceeds maximum"

**Cause**: XSD file too large

**Solution**:
Update `application.yml`:
```yaml
dynamic-service:
  xsd:
    max-file-size: 10485760  # 10MB
```

### Issue: "Schema validation failed"

**Cause**: Invalid XSD syntax or security issues

**Solution**:
- Validate XSD with online validator
- Remove DOCTYPE declarations
- Remove external entity references

### Enable Debug Logging

```yaml
logging:
  level:
    com.dynamic: DEBUG
    org.springframework: DEBUG
```

## 📊 Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

### Prometheus Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

## 🔒 Security Considerations

- **XXE Prevention**: External entities are disabled
- **XML Bomb Prevention**: Entity expansion limits enforced
- **File Size Limits**: Configurable maximum file size
- **Input Validation**: All inputs validated with Bean Validation
- **Audit Logging**: All operations logged to database

## 🚢 Production Deployment

### Using JAR

```bash
java -Xmx2g -jar \
  -Dspring.profiles.active=prod \
  -Dserver.port=8080 \
  target/xsd-service-platform-1.0.0.jar
```

### Using Docker (Future)

```bash
docker build -t xsd-platform .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod xsd-platform
```

### Environment Variables

```bash
export DB_HOST=postgres-server
export DB_PORT=5432
export DB_NAME=xsdplatform
export DB_USERNAME=dbuser
export DB_PASSWORD=dbpass
export JWT_SECRET=your-secret-key
```

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📧 Support

For issues and questions:
- Open an issue on GitHub
- Check existing documentation
- Review troubleshooting section

## 🎯 Roadmap

### Phase 1 (Current)
- ✅ XSD validation and parsing
- ✅ POJO generation with JAXB
- ✅ Dynamic compilation
- ✅ Custom classloader management
- ✅ Schema management APIs

### Phase 2 (Next)
- 🔲 Dynamic REST controller generation
- 🔲 Dynamic SOAP endpoint generation
- 🔲 Service deployment orchestration
- 🔲 WSDL generation
- 🔲 OpenAPI documentation generation

### Phase 3 (Future)
- 🔲 React frontend with TypeScript
- 🔲 User authentication and authorization
- 🔲 Service versioning
- 🔲 Hot reload support
- 🔲 GraphQL API generation
- 🔲 Advanced caching strategies

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- JAXB team for XML binding capabilities
- Woodstox for high-performance XML processing
- All contributors and users of this platform

---

**Built with ❤️ using Java 21 and Spring Boot 3.x**

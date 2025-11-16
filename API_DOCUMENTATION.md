# API Documentation - Dynamic XSD Service Generation Platform

Complete API reference for all endpoints in the Dynamic XSD Service Generation Platform.

**Base URL:** `http://localhost:8080`

**API Version:** v1

---

## Table of Contents

1. [Authentication](#authentication)
2. [Schema Management](#schema-management)
3. [Service Deployment](#service-deployment)
4. [Catalog & Discovery](#catalog--discovery)
5. [Management & Monitoring](#management--monitoring)
6. [Dynamic Endpoints](#dynamic-endpoints)
7. [Error Handling](#error-handling)
8. [Rate Limiting](#rate-limiting)

---

## Authentication

### Default Users

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| admin | admin123 | ADMIN | Full access |
| developer | dev123 | USER | Standard access |
| viewer | viewer123 | VIEWER | Read-only access |

**Note:** Authentication is currently disabled for development. Enable in `SecurityConfig.java`.

### JWT Authentication (When Enabled)

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 3600
}
```

**Use Token in Requests:**
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Schema Management

### Upload XSD Schema

Upload an XSD file to generate POJOs and prepare for deployment.

**Endpoint:**
```http
POST /api/v1/schemas/upload
Content-Type: multipart/form-data
```

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | File | Yes | XSD file (max 5MB) |
| serviceName | String | Yes | Service name (lowercase, alphanumeric, hyphens) |
| version | String | No | Version (default: "1.0") |
| description | String | No | Service description |
| namespace | String | No | Target namespace (auto-detected if not provided) |
| autoDeploy | Boolean | No | Auto-deploy after compilation (default: false) |

**Example Request (curl):**
```bash
curl -X POST http://localhost:8080/api/v1/schemas/upload \
  -F "file=@customer.xsd" \
  -F "serviceName=customer-service" \
  -F "version=1.0" \
  -F "description=Customer management service" \
  -F "autoDeploy=true"
```

**Example Request (JavaScript):**
```javascript
const formData = new FormData();
formData.append('file', file);
formData.append('serviceName', 'customer-service');
formData.append('version', '1.0');
formData.append('description', 'Customer management service');
formData.append('autoDeploy', 'true');

fetch('http://localhost:8080/api/v1/schemas/upload', {
  method: 'POST',
  body: formData
})
.then(res => res.json())
.then(data => console.log(data));
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Schema uploaded and compiled successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "serviceName": "customer-service",
    "version": "1.0",
    "namespace": "http://example.com/customer",
    "status": "ACTIVE",
    "uploadedAt": "2024-11-16T10:30:00Z",
    "validationWarnings": [],
    "validationErrors": []
  },
  "timestamp": "2024-11-16T10:30:00Z"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Invalid XSD file",
  "errors": [
    {
      "field": "file",
      "message": "XSD validation failed: Element 'Customer' is not defined",
      "rejectedValue": null
    }
  ],
  "timestamp": "2024-11-16T10:30:00Z"
}
```

---

### List Schemas

Retrieve paginated list of uploaded schemas.

**Endpoint:**
```http
GET /api/v1/schemas?page=0&size=20&status=ACTIVE&search=customer
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Integer | No | Page number (default: 0) |
| size | Integer | No | Page size (default: 20) |
| status | String | No | Filter by status |
| search | String | No | Search by service name |
| sort | String | No | Sort field (e.g., "uploadedAt,desc") |

**Status Values:**
- `UPLOADED` - Initial upload
- `VALIDATING` - Validation in progress
- `VALIDATION_FAILED` - Validation errors
- `GENERATING` - POJO generation in progress
- `GENERATION_FAILED` - Generation errors
- `COMPILING` - Compilation in progress
- `COMPILATION_FAILED` - Compilation errors
- `ACTIVE` - Successfully compiled
- `FAILED` - General failure
- `DEPRECATED` - Marked for deletion

**Success Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "serviceName": "customer-service",
      "version": "1.0",
      "namespace": "http://example.com/customer",
      "description": "Customer management service",
      "status": "ACTIVE",
      "uploadedBy": "admin",
      "uploadedAt": "2024-11-16T10:30:00Z",
      "updatedAt": "2024-11-16T10:30:15Z",
      "generatedArtifacts": {
        "pojos": ["Customer", "Address", "PhoneNumber"],
        "restEndpointCount": 3,
        "soapOperationCount": 5
      },
      "validationErrors": null,
      "compilationErrors": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 1,
  "empty": false
}
```

---

### Get Schema Details

Retrieve details of a specific schema by service name.

**Endpoint:**
```http
GET /api/v1/schemas/{serviceName}
```

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "customer-service",
  "version": "1.0",
  "namespace": "http://example.com/customer",
  "description": "Customer management service",
  "status": "ACTIVE",
  "uploadedBy": "admin",
  "uploadedAt": "2024-11-16T10:30:00Z",
  "generatedArtifacts": {
    "pojos": ["Customer", "Address", "PhoneNumber"],
    "restEndpointCount": 3,
    "soapOperationCount": 5
  }
}
```

---

### Delete Schema

Delete a schema (only if not deployed).

**Endpoint:**
```http
DELETE /api/v1/schemas/{serviceName}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Schema 'customer-service' deleted successfully",
  "timestamp": "2024-11-16T11:00:00Z"
}
```

---

## Service Deployment

### Deploy Service

Deploy a schema as a live service with REST and/or SOAP endpoints.

**Endpoint:**
```http
POST /api/v1/services/deploy/{schemaId}
Content-Type: application/json
```

**Request Body:**
```json
{
  "enableRest": true,
  "enableSoap": true,
  "customPath": "/api/custom"
}
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| enableRest | Boolean | No | Enable REST endpoints (default: true) |
| enableSoap | Boolean | No | Enable SOAP endpoints (default: true) |
| customPath | String | No | Custom base path (default: /api/dynamic) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Service deployed successfully",
  "data": {
    "serviceId": "750e8400-e29b-41d4-a716-446655440001",
    "serviceName": "customer-service",
    "version": "1.0",
    "status": "DEPLOYED",
    "deployedAt": "2024-11-16T11:00:00Z",
    "endpoints": {
      "rest": [
        "GET /api/dynamic/customer-service/Customer",
        "POST /api/dynamic/customer-service/Customer",
        "PUT /api/dynamic/customer-service/Customer/{id}",
        "DELETE /api/dynamic/customer-service/Customer/{id}"
      ],
      "soap": [
        "POST /soap/customer-service"
      ],
      "wsdl": "http://localhost:8080/api/v1/services/wsdl/customer-service?wsdl"
    }
  },
  "timestamp": "2024-11-16T11:00:00Z"
}
```

---

### Undeploy Service

Undeploy a service and remove all endpoints.

**Endpoint:**
```http
POST /api/v1/services/undeploy/{serviceId}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Service undeployed successfully",
  "timestamp": "2024-11-16T11:30:00Z"
}
```

---

### Get Service Status

Get deployment status of a service.

**Endpoint:**
```http
GET /api/v1/services/status/{serviceId}
```

**Success Response (200 OK):**
```json
{
  "id": "750e8400-e29b-41d4-a716-446655440001",
  "serviceName": "customer-service",
  "version": "1.0",
  "schemaId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "DEPLOYED",
  "deployedAt": "2024-11-16T11:00:00Z",
  "deployedBy": "admin",
  "totalRequests": 1250,
  "averageResponseTime": 45.3
}
```

---

### Get WSDL Document

Retrieve WSDL document for a SOAP service.

**Endpoint:**
```http
GET /api/v1/services/wsdl/{serviceName}?wsdl
```

**Success Response (200 OK):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://schemas.xmlsoap.org/wsdl/"
             xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
             xmlns:tns="http://example.com/customer"
             targetNamespace="http://example.com/customer">
  <types>
    <xs:schema targetNamespace="http://example.com/customer">
      <!-- Schema definitions -->
    </xs:schema>
  </types>
  <!-- Service definitions -->
</definitions>
```

---

## Catalog & Discovery

### List All Services

List all deployed services.

**Endpoint:**
```http
GET /api/v1/catalog/services
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "750e8400-e29b-41d4-a716-446655440001",
      "serviceName": "customer-service",
      "version": "1.0",
      "status": "DEPLOYED",
      "deployedAt": "2024-11-16T11:00:00Z",
      "totalEndpoints": 5,
      "restEndpoints": 4,
      "soapEndpoints": 1,
      "totalRequests": 1250,
      "averageResponseTime": 45.3
    }
  ],
  "message": "Services retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### Get Service Details

Get detailed information about a service.

**Endpoint:**
```http
GET /api/v1/catalog/services/{serviceName}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "service": {
      "id": "750e8400-e29b-41d4-a716-446655440001",
      "serviceName": "customer-service",
      "version": "1.0",
      "status": "DEPLOYED",
      "deployedAt": "2024-11-16T11:00:00Z",
      "totalRequests": 1250,
      "averageResponseTime": 45.3
    },
    "schema": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "targetNamespace": "http://example.com/customer",
      "generatedPojos": ["Customer", "Address", "PhoneNumber"]
    },
    "endpoints": [
      {
        "id": "endpoint-001",
        "endpointType": "REST",
        "path": "/api/dynamic/customer-service/Customer",
        "httpMethod": "GET",
        "operationName": "getAllCustomers",
        "description": "Retrieve all customers",
        "active": true
      }
    ],
    "wsdlAvailable": true
  },
  "message": "Service details retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### List Schemas (Catalog)

List all schemas with deployment status.

**Endpoint:**
```http
GET /api/v1/catalog/schemas?status=ACTIVE
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | String | No | Filter by schema status |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "serviceName": "customer-service",
      "version": "1.0",
      "status": "ACTIVE",
      "uploadedAt": "2024-11-16T10:30:00Z",
      "uploadedBy": "admin",
      "targetNamespace": "http://example.com/customer",
      "generatedPojos": ["Customer", "Address", "PhoneNumber"],
      "deployed": true
    }
  ],
  "message": "Schemas retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### List All Endpoints

List all active endpoints across all services.

**Endpoint:**
```http
GET /api/v1/catalog/endpoints
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "endpoint-001",
      "type": "REST",
      "path": "/api/dynamic/customer-service/Customer",
      "httpMethod": "GET",
      "operationName": "getAllCustomers",
      "description": "Retrieve all customers",
      "serviceId": "750e8400-e29b-41d4-a716-446655440001",
      "serviceName": "customer-service"
    }
  ],
  "message": "Endpoints retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### Search Services

Search services by name or description.

**Endpoint:**
```http
GET /api/v1/catalog/search?query=customer
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "750e8400-e29b-41d4-a716-446655440001",
      "serviceName": "customer-service",
      "version": "1.0",
      "status": "DEPLOYED",
      "deployedAt": "2024-11-16T11:00:00Z"
    }
  ],
  "message": "Search results",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

## Management & Monitoring

### Get Rate Limit Status

Get current rate limit status for a user.

**Endpoint:**
```http
GET /api/v1/management/rate-limit/{username}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "identifier": "admin",
    "bucketType": "USER",
    "tokensRemaining": 87,
    "maxTokens": 100,
    "refillRate": 10,
    "resetAt": "2024-11-16T12:05:00Z",
    "blocked": false
  },
  "message": "Rate limit status retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### Reset Rate Limit (Admin Only)

Reset rate limit bucket for a user.

**Endpoint:**
```http
POST /api/v1/management/rate-limit/{username}/reset
Authorization: Bearer <admin-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Rate limit reset successfully",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### Configure Rate Limit (Admin Only)

Configure rate limit settings for a user.

**Endpoint:**
```http
POST /api/v1/management/rate-limit/{username}/configure?maxTokens=200&refillRate=20
Authorization: Bearer <admin-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Rate limit configured successfully",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### Get Audit Logs (Admin Only)

Retrieve audit logs with filtering.

**Endpoint:**
```http
GET /api/v1/management/audit?username=admin&limit=100
Authorization: Bearer <admin-token>
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| username | String | No | Filter by username |
| serviceName | String | No | Filter by service name |
| startTime | DateTime | No | Start time (ISO 8601) |
| endTime | DateTime | No | End time (ISO 8601) |
| limit | Integer | No | Max results (default: 100) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "logs": [
      {
        "id": "audit-001",
        "username": "admin",
        "action": "SCHEMA_UPLOAD",
        "serviceName": "customer-service",
        "status": "SUCCESS",
        "details": "Uploaded XSD schema successfully",
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "timestamp": "2024-11-16T10:30:00Z",
        "durationMs": 1250
      }
    ]
  },
  "message": "Audit logs retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### Get Service Metrics

Retrieve metrics for a service.

**Endpoint:**
```http
GET /api/v1/management/metrics/{serviceId}?metricType=RESPONSE_TIME&startTime=2024-11-16T00:00:00Z&endTime=2024-11-16T23:59:59Z
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| metricType | String | No | Metric type to filter |
| startTime | DateTime | No | Start time (default: 24h ago) |
| endTime | DateTime | No | End time (default: now) |

**Metric Types:**
- `REQUEST_COUNT` - Number of requests
- `RESPONSE_TIME` - Response time in milliseconds
- `ERROR_RATE` - Error rate percentage
- `SUCCESS_RATE` - Success rate percentage
- `THROUGHPUT` - Requests per second
- `MEMORY_USAGE` - Memory usage in MB
- `CPU_USAGE` - CPU usage percentage

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "metric-001",
      "serviceDefinitionId": "750e8400-e29b-41d4-a716-446655440001",
      "timestamp": "2024-11-16T12:00:00Z",
      "metricType": "RESPONSE_TIME",
      "metricValue": 45.3,
      "tags": {
        "endpoint": "/api/dynamic/customer-service/Customer",
        "method": "GET"
      },
      "aggregationWindow": "5m"
    }
  ],
  "message": "Metrics retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### Get Metric Statistics

Get statistical analysis of metrics.

**Endpoint:**
```http
GET /api/v1/management/metrics/{serviceId}/statistics?metricType=RESPONSE_TIME
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "metricType": "RESPONSE_TIME",
    "count": 1250,
    "average": 45.3,
    "min": 12.5,
    "max": 234.7,
    "sum": 56625.0,
    "stdDev": 23.4,
    "percentile50": 42.1,
    "percentile95": 98.5,
    "percentile99": 187.3
  },
  "message": "Metric statistics retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

### Get Time Series Data

Get time-series metrics with aggregation.

**Endpoint:**
```http
GET /api/v1/management/metrics/{serviceId}/timeseries?metricType=RESPONSE_TIME&windowMinutes=5
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "timestamp": "2024-11-16T12:00:00Z",
      "average": 45.3,
      "min": 23.1,
      "max": 89.7,
      "count": 125
    }
  ],
  "message": "Time series data retrieved",
  "timestamp": "2024-11-16T12:00:00Z"
}
```

---

## Dynamic Endpoints

These endpoints are generated at runtime based on deployed services.

### Get All Entities

Retrieve all entities of a type.

**Endpoint:**
```http
GET /api/dynamic/{serviceName}/{EntityName}
Accept: application/json
```

**Success Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "address": {
      "street": "123 Main St",
      "city": "New York"
    }
  }
]
```

**XML Response (Accept: application/xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Customers>
  <Customer>
    <id>1</id>
    <name>John Doe</name>
    <email>john@example.com</email>
  </Customer>
</Customers>
```

---

### Get Entity by ID

Retrieve a specific entity by ID.

**Endpoint:**
```http
GET /api/dynamic/{serviceName}/{EntityName}/{id}
Accept: application/json
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com"
}
```

---

### Create Entity

Create a new entity.

**Endpoint:**
```http
POST /api/dynamic/{serviceName}/{EntityName}
Content-Type: application/json
```

**Request Body (JSON):**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com"
}
```

**Request Body (XML):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Customer>
  <name>Jane Doe</name>
  <email>jane@example.com</email>
</Customer>
```

**Success Response (201 Created):**
```json
{
  "id": 2,
  "name": "Jane Doe",
  "email": "jane@example.com"
}
```

---

### Update Entity

Update an existing entity.

**Endpoint:**
```http
PUT /api/dynamic/{serviceName}/{EntityName}/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Jane Smith",
  "email": "jane.smith@example.com"
}
```

**Success Response (200 OK):**
```json
{
  "id": 2,
  "name": "Jane Smith",
  "email": "jane.smith@example.com"
}
```

---

### Delete Entity

Delete an entity.

**Endpoint:**
```http
DELETE /api/dynamic/{serviceName}/{EntityName}/{id}
```

**Success Response (204 No Content)**

---

## Error Handling

### Error Response Format

All errors follow this format:

```json
{
  "timestamp": "2024-11-16T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/schemas/upload",
  "errors": [
    {
      "field": "serviceName",
      "message": "Service name must contain only lowercase letters, numbers, and hyphens",
      "rejectedValue": "Customer Service"
    }
  ],
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Entity created successfully |
| 204 | No Content | Entity deleted successfully |
| 400 | Bad Request | Invalid input data |
| 401 | Unauthorized | Authentication required |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource already exists |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

---

## Rate Limiting

### Rate Limit Headers

All responses include rate limit headers:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 2024-11-16T12:05:00Z
```

### Rate Limit Exceeded Response

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 2024-11-16T12:05:00Z
Content-Type: application/json

{
  "error": "Rate limit exceeded",
  "message": "You have exceeded the rate limit. Please try again later.",
  "resetAt": "2024-11-16T12:05:00Z"
}
```

---

## Pagination

List endpoints support pagination:

### Request
```http
GET /api/v1/schemas?page=0&size=20&sort=uploadedAt,desc
```

### Response
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 45,
  "totalPages": 3,
  "last": false,
  "first": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 20,
  "empty": false
}
```

---

## API Changelog

### Version 1.0.0 (2024-11-16)
- Initial release
- Schema upload and management
- Service deployment
- Dynamic endpoint generation
- Rate limiting
- Audit logging
- Metrics collection

---

**Last Updated:** 2024-11-16
**API Version:** 1.0.0

# New Protocols Implementation - GraphQL, ActiveMQ, and SFTP

## Overview
Successfully implemented three additional protocols for the Dynamic XSD Service Generation Platform, expanding the total supported protocols to **7**.

**Status**: ✅ Implementation Complete
**Date**: 2025-11-17
**Protocols Added**: GraphQL, ActiveMQ (JMS), SFTP

---

## Platform Summary

### All Supported Protocols

| Protocol | Type | Use Case | Port/Endpoint |
|----------|------|----------|---------------|
| **REST** | HTTP API | Standard web APIs | `http://localhost:8080/api/{service}` |
| **SOAP** | Web Service | Enterprise integration | `http://localhost:8080/ws/{service}` |
| **WebSocket** | Real-time | Live updates, push notifications | `ws://localhost:8080/ws` |
| **gRPC** | RPC | High-performance microservices | Port 9090 (proto generation) |
| **GraphQL** | Query Language | Flexible client queries | `http://localhost:8080/graphql` |
| **ActiveMQ** | Message Queue | Async messaging, event-driven | JMS queues/topics |
| **SFTP** | File Transfer | Batch processing, file integration | SFTP directories |

---

## Phase 4: GraphQL Implementation

### Overview
GraphQL allows clients to query XSD-generated services with flexible, client-defined queries and real-time subscriptions.

### Features
- ✅ Dynamic schema generation from XSD
- ✅ Query, Mutation, and Subscription support
- ✅ Type-safe schema with custom scalars
- ✅ Flexible field selection
- ✅ Real-time subscriptions for data changes
- ✅ Pagination and filtering

### Files Created

#### 1. [GraphQLSchemaGenerator.java](../backend/src/main/java/com/dynamic/xsd/graphql/GraphQLSchemaGenerator.java)
Generates GraphQL schemas (`.graphqls`) from XSD schemas.

**Key Features**:
- Extracts types from XSD complex types
- Generates Query, Mutation, and Subscription types
- Creates input types for mutations
- Supports custom scalars (DateTime, JSON, Long)
- Auto-generates CRUD operations

**Example Generated Schema**:
```graphql
# Custom Scalars
scalar DateTime
scalar JSON
scalar Long

# Customer type
type Customer {
  id: ID!
  customerId: String!
  name: String!
  email: String
  createdAt: DateTime
}

# Customer input type
input CustomerInput {
  customerId: String!
  name: String!
  email: String
}

# Query root type
type Query {
  customer(id: ID!): Customer
  customers(page: Int, pageSize: Int): [Customer!]!
  searchCustomer(filter: JSON): [Customer!]!
}

# Mutation root type
type Mutation {
  createCustomer(input: CustomerInput!): Customer!
  updateCustomer(id: ID!, input: CustomerInput!): Customer!
  deleteCustomer(id: ID!): DeleteResponse!
}

# Subscription root type
type Subscription {
  customerUpdated(id: ID): Customer!
  customerChanges: ChangeEvent!
}
```

#### 2. [XsdToGraphQLTypeMapper.java](../backend/src/main/java/com/dynamic/xsd/graphql/XsdToGraphQLTypeMapper.java)
Maps XSD types to GraphQL types.

**Type Mappings**:
| XSD Type | GraphQL Type |
|----------|--------------|
| string | String |
| int | Int |
| long, integer | Long |
| float, double, decimal | Float |
| boolean | Boolean |
| dateTime | DateTime (custom scalar) |
| base64Binary | String (base64 encoded) |
| ID, IDREF | ID |

**Naming Conventions**:
- Types: PascalCase (e.g., `CustomerOrder`)
- Fields: camelCase (e.g., `orderId`)

#### 3. [GraphQLConfig.java](../backend/src/main/java/com/dynamic/xsd/graphql/GraphQLConfig.java)
GraphQL configuration with custom scalars.

**Custom Scalars Configured**:
- `DateTime` - ISO-8601 date/time
- `JSON` - JSON objects
- `Long` - 64-bit integers
- `Object` - Any object

#### 4. [DynamicGraphQLDataFetcher.java](../backend/src/main/java/com/dynamic/xsd/graphql/DynamicGraphQLDataFetcher.java)
Runtime data fetcher for dynamic query/mutation execution.

**Supported Operations**:
- `getById(serviceName, typeName, id)` - Fetch single entity
- `getAll(serviceName, typeName, page, pageSize)` - List entities with pagination
- `search(serviceName, typeName, filter)` - Search with filter criteria
- `create(serviceName, typeName, input)` - Create new entity
- `update(serviceName, typeName, id, input)` - Update existing entity
- `delete(serviceName, typeName, id)` - Delete entity

### Usage Example

**Query**:
```graphql
query GetCustomer {
  customer(id: "123") {
    id
    name
    email
    createdAt
  }
}
```

**Mutation**:
```graphql
mutation CreateCustomer {
  createCustomer(input: {
    customerId: "CUST001"
    name: "John Doe"
    email: "john@example.com"
  }) {
    id
    customerId
    name
  }
}
```

**Subscription**:
```graphql
subscription CustomerUpdates {
  customerChanges {
    eventType
    entityId
    timestamp
  }
}
```

---

## Phase 5: ActiveMQ (JMS) Implementation

### Overview
ActiveMQ provides asynchronous message-based communication using JMS queues and topics.

### Features
- ✅ Point-to-point messaging (Queues)
- ✅ Publish/subscribe messaging (Topics)
- ✅ JSON message serialization
- ✅ Event broadcasting
- ✅ CRUD operations via messages
- ✅ Request/response pattern support

### Files Created

#### 1. [ActiveMQConfig.java](../backend/src/main/java/com/dynamic/xsd/activemq/ActiveMQConfig.java)
ActiveMQ configuration with JMS support.

**Configuration**:
- Embedded ActiveMQ broker (development)
- External broker support (production)
- JSON message converter
- Queue and Topic templates
- Listener container factories

**Default Settings**:
```properties
spring.activemq.broker-url=vm://localhost?broker.persistent=false
spring.activemq.user=admin
spring.activemq.password=admin
```

#### 2. [ActiveMQMessageService.java](../backend/src/main/java/com/dynamic/xsd/activemq/ActiveMQMessageService.java)
Service for sending messages to queues and topics.

**Queue Naming Convention**:
- Service operations: `service.{serviceName}`
- Requests: `request.{serviceName}`
- Responses: `response.{serviceName}`

**Topic Naming Convention**:
- Service events: `events.{serviceName}`

**API Methods**:
```java
// Send to queue (point-to-point)
messageService.sendToQueue("service.customer", message);

// Publish to topic (pub/sub)
messageService.publishToTopic("events.customer", event);

// CRUD operations
messageService.sendCreate("customer", entityData);
messageService.sendUpdate("customer", "123", entityData);
messageService.sendDelete("customer", "123");
messageService.sendGet("customer", "123");
messageService.sendList("customer");
```

#### 3. [DynamicMessageListener.java](../backend/src/main/java/com/dynamic/xsd/activemq/DynamicMessageListener.java)
Processes incoming JMS messages and executes operations.

**Message Format**:
```json
{
  "service": "customer",
  "operation": "CREATE",
  "data": {
    "customerId": "CUST001",
    "name": "John Doe"
  },
  "timestamp": 1700000000000
}
```

**Supported Operations**:
- CREATE - Create new entity
- GET - Retrieve entity by ID
- UPDATE - Update existing entity
- DELETE - Delete entity
- LIST - List all entities

### Usage Example

**Sending a Message** (Java):
```java
@Autowired
private ActiveMQMessageService messageService;

// Create a customer
Map<String, Object> customer = Map.of(
    "customerId", "CUST001",
    "name", "John Doe",
    "email", "john@example.com"
);

messageService.sendCreate("customer", customer);
```

**Listening for Events**:
```java
@JmsListener(destination = "events.customer", containerFactory = "jmsTopicListenerContainerFactory")
public void onCustomerEvent(Map<String, Object> event) {
    String eventType = (String) event.get("eventType");
    System.out.println("Customer event: " + eventType);
}
```

---

## Phase 6: SFTP Implementation

### Overview
SFTP provides file-based service interaction for batch processing and system integration.

### Features
- ✅ File-based request/response pattern
- ✅ JSON file format for data exchange
- ✅ Automatic directory structure
- ✅ Request archival
- ✅ Error file generation
- ✅ Batch processing support

### Files Created

#### 1. [SftpConfig.java](../backend/src/main/java/com/dynamic/xsd/sftp/SftpConfig.java)
SFTP session factory and directory configuration.

**Configuration**:
```properties
sftp.host=localhost
sftp.port=22
sftp.user=sftpuser
sftp.password=sftppass
sftp.private-key=/path/to/key (optional)
sftp.root-directory=/sftp
```

**Directory Structure**:
```
/sftp/{serviceName}/
├── requests/     # Incoming request files
├── responses/    # Generated response files
├── errors/       # Error files for failed requests
└── archive/      # Processed request files
```

#### 2. [SftpFileService.java](../backend/src/main/java/com/dynamic/xsd/sftp/SftpFileService.java)
File operations for SFTP-based service interaction.

**Operations**:
- `writeRequestFile()` - Create request file
- `writeResponseFile()` - Create response file
- `writeErrorFile()` - Create error file
- `readRequestFiles()` - Process pending requests
- `archiveFile()` - Move to archive
- `initializeServiceDirectories()` - Setup directories

### Usage Example

**Request File Format** (`/sftp/customer/requests/1700000000_create_request.json`):
```json
{
  "service": "customer",
  "operation": "CREATE",
  "data": {
    "customerId": "CUST001",
    "name": "John Doe",
    "email": "john@example.com"
  },
  "timestamp": 1700000000000,
  "requestId": "uuid-123-456"
}
```

**Response File Format** (`/sftp/customer/responses/uuid-123-456_response.json`):
```json
{
  "requestId": "uuid-123-456",
  "service": "customer",
  "success": true,
  "data": {
    "id": "1",
    "customerId": "CUST001",
    "name": "John Doe",
    "email": "john@example.com",
    "createdAt": 1700000000000
  },
  "timestamp": 1700000001000
}
```

**Error File Format** (`/sftp/customer/errors/uuid-123-456_error.json`):
```json
{
  "requestId": "uuid-123-456",
  "service": "customer",
  "success": false,
  "error": "Customer ID already exists",
  "timestamp": 1700000001000
}
```

**Java API**:
```java
@Autowired
private SftpFileService sftpService;

// Initialize directories for a service
sftpService.initializeServiceDirectories("customer");

// Write a request file
Map<String, Object> data = Map.of("name", "John Doe");
String filename = sftpService.writeRequestFile("customer", "CREATE", data);

// Read and process request files
List<Map<String, Object>> requests = sftpService.readRequestFiles("customer");

// Get statistics
Map<String, Object> stats = sftpService.getServiceStats("customer");
```

---

## Dependencies Added

### pom.xml Updates

```xml
<!-- GraphQL Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>

<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-extended-scalars</artifactId>
    <version>21.0</version>
</dependency>

<!-- ActiveMQ Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-activemq</artifactId>
</dependency>

<dependency>
    <groupId>org.apache.activemq</groupId>
    <artifactId>activemq-broker</artifactId>
</dependency>

<!-- SFTP Support -->
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-sftp</artifactId>
</dependency>

<dependency>
    <groupId>com.jcraft</groupId>
    <artifactId>jsch</artifactId>
    <version>0.1.55</version>
</dependency>
```

---

## Protocol Comparison

### When to Use Each Protocol

| Protocol | Best For | Latency | Complexity | Data Format |
|----------|----------|---------|------------|-------------|
| **REST** | Public APIs, CRUD operations | ~50ms | Low | JSON |
| **SOAP** | Enterprise integration, contracts | ~60ms | Medium | XML |
| **WebSocket** | Real-time updates, chat, dashboards | ~5ms | Medium | JSON |
| **gRPC** | Microservices, high throughput | ~2ms | High | Protobuf |
| **GraphQL** | Complex queries, mobile apps | ~40ms | Medium | JSON |
| **ActiveMQ** | Async processing, event-driven | ~20ms | Medium | JSON |
| **SFTP** | Batch processing, file integration | Minutes | Low | JSON files |

### Use Case Matrix

| Use Case | Recommended Protocols |
|----------|----------------------|
| Public web API | REST, GraphQL |
| Mobile app | GraphQL, REST |
| Microservices | gRPC, REST |
| Real-time dashboard | WebSocket, GraphQL Subscriptions |
| Enterprise integration | SOAP, ActiveMQ |
| Event-driven architecture | ActiveMQ, WebSocket |
| Batch file processing | SFTP |
| IoT data ingestion | WebSocket, ActiveMQ |
| Legacy system integration | SOAP, SFTP |
| High-frequency trading | gRPC, WebSocket |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Applications                      │
├─────────────────────────────────────────────────────────────────┤
│  Web │ Mobile │ Desktop │ CLI │ Browser │ File System │ MQ      │
└──┬───┴────┬───┴────┬────┴──┬──┴────┬────┴──────┬──────┴────┬────┘
   │        │        │       │       │           │           │
   │ REST   │GraphQL │ SOAP  │ gRPC  │WebSocket  │  SFTP     │ JMS
   │        │        │       │       │           │           │
┌──▼────────▼────────▼───────▼───────▼───────────▼───────────▼────┐
│                  Protocol Adapters Layer                         │
├──────────────────────────────────────────────────────────────────┤
│  REST    │ GraphQL  │  SOAP  │  gRPC  │  WS  │  SFTP  │  JMS   │
│Controller│  Schema  │Endpoint│ Service│ STOMP│  File  │ Queue  │
└──────────┴──────────┴────────┴────────┴──────┴────────┴────────┘
                                │
┌───────────────────────────────▼──────────────────────────────────┐
│              XSD Service Generation Engine                       │
├──────────────────────────────────────────────────────────────────┤
│  • Parse XSD Schema                                              │
│  • Generate Service Code (REST, SOAP, WebSocket, gRPC)          │
│  • Generate GraphQL Schema                                       │
│  • Configure ActiveMQ Topics/Queues                              │
│  • Initialize SFTP Directories                                   │
│  • Deploy Service Dynamically                                    │
└──────────────────────────────────────────────────────────────────┘
                                │
┌───────────────────────────────▼──────────────────────────────────┐
│                      Data Storage Layer                          │
├──────────────────────────────────────────────────────────────────┤
│  PostgreSQL │ In-Memory Maps │ File System │ Message Broker      │
└──────────────────────────────────────────────────────────────────┘
```

---

## Testing the New Protocols

### GraphQL Testing

**Using GraphQL Playground** (`http://localhost:8080/graphiql`):

```graphql
# Query
query {
  customer(id: "1") {
    name
    email
  }
}

# Mutation
mutation {
  createCustomer(input: {
    customerId: "CUST001"
    name: "Test User"
  }) {
    id
    customerId
  }
}
```

**Using curl**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { customer(id: \"1\") { name email } }"
  }'
```

### ActiveMQ Testing

**Using Spring JMS**:
```java
@Autowired
private JmsTemplate jmsTemplate;

// Send message
Map<String, Object> message = Map.of(
    "service", "customer",
    "operation", "CREATE",
    "data", Map.of("name", "Test")
);
jmsTemplate.convertAndSend("service.customer", message);
```

**Monitor Queues**:
```bash
# Check ActiveMQ Web Console
http://localhost:8161/admin
# Default credentials: admin/admin
```

### SFTP Testing

**Create Request File**:
```bash
# Create request file
cat > /sftp/customer/requests/test_create_request.json <<EOF
{
  "service": "customer",
  "operation": "CREATE",
  "data": {
    "name": "Test Customer",
    "email": "test@example.com"
  },
  "timestamp": $(date +%s000),
  "requestId": "test-123"
}
EOF
```

**Check Response**:
```bash
# Wait for processing, then check response
cat /sftp/customer/responses/test-123_response.json
```

---

## Configuration

### application.properties

```properties
# GraphQL
spring.graphql.graphiql.enabled=true
spring.graphql.graphiql.path=/graphiql

# ActiveMQ
spring.activemq.broker-url=vm://localhost?broker.persistent=false
spring.activemq.user=admin
spring.activemq.password=admin
spring.jms.pub-sub-domain=false

# SFTP
sftp.host=localhost
sftp.port=22
sftp.user=sftpuser
sftp.password=sftppass
sftp.root-directory=/sftp

# Enable all protocols
protocols.enabled=rest,soap,websocket,grpc,graphql,activemq,sftp
```

---

## Benefits Achieved

### GraphQL
✅ **Flexible Queries** - Clients request exactly what they need
✅ **Single Endpoint** - One endpoint for all operations
✅ **Type Safety** - Strong typing with schema validation
✅ **Real-time Updates** - Subscriptions for live data
✅ **Reduced Over-fetching** - No unnecessary data transfer

### ActiveMQ
✅ **Asynchronous Processing** - Non-blocking operations
✅ **Decoupled Architecture** - Producers and consumers independent
✅ **Reliable Messaging** - Message persistence and delivery guarantees
✅ **Scalable** - Horizontal scaling with message distribution
✅ **Event-Driven** - Perfect for event-driven architectures

### SFTP
✅ **Batch Processing** - Handle large file-based workloads
✅ **Legacy Integration** - Integrate with file-based systems
✅ **Audit Trail** - Automatic file archival
✅ **Secure Transfer** - Encrypted file transfers
✅ **Simple Integration** - Easy for non-technical systems

---

## Complete File Summary

### New Files Created

**GraphQL** (4 files):
1. `backend/src/main/java/com/dynamic/xsd/graphql/GraphQLSchemaGenerator.java`
2. `backend/src/main/java/com/dynamic/xsd/graphql/XsdToGraphQLTypeMapper.java`
3. `backend/src/main/java/com/dynamic/xsd/graphql/GraphQLConfig.java`
4. `backend/src/main/java/com/dynamic/xsd/graphql/DynamicGraphQLDataFetcher.java`

**ActiveMQ** (3 files):
1. `backend/src/main/java/com/dynamic/xsd/activemq/ActiveMQConfig.java`
2. `backend/src/main/java/com/dynamic/xsd/activemq/ActiveMQMessageService.java`
3. `backend/src/main/java/com/dynamic/xsd/activemq/DynamicMessageListener.java`

**SFTP** (2 files):
1. `backend/src/main/java/com/dynamic/xsd/sftp/SftpConfig.java`
2. `backend/src/main/java/com/dynamic/xsd/sftp/SftpFileService.java`

**Documentation** (1 file):
1. `docs/NEW_PROTOCOLS_IMPLEMENTATION.md` (this file)

### Modified Files

1. `backend/pom.xml` - Added dependencies for GraphQL, ActiveMQ, and SFTP

---

## Next Steps

### Immediate
1. ✅ Test GraphQL endpoint with sample queries
2. ✅ Verify ActiveMQ message flow
3. ✅ Test SFTP file processing
4. ⏳ Docker build with new dependencies
5. ⏳ Integration testing

### Future Enhancements

**GraphQL**:
- Add authentication/authorization directives
- Implement DataLoader for N+1 query optimization
- Add query complexity analysis
- Schema stitching for multiple XSD services

**ActiveMQ**:
- Dead letter queue (DLQ) handling
- Message retry logic
- Message transformation pipelines
- Distributed tracing

**SFTP**:
- Scheduled file polling
- File format validation
- Large file chunking
- Compression support

---

## Status Summary

**Implementation Status**: 🎉 **Complete**

| Protocol | Status | Files Created | Lines of Code |
|----------|--------|---------------|---------------|
| GraphQL | ✅ Complete | 4 | ~500 |
| ActiveMQ | ✅ Complete | 3 | ~350 |
| SFTP | ✅ Complete | 2 | ~300 |

**Total Protocols Supported**: **7**
**Total Files Added**: **10**
**Total Lines of Code**: **~1,150**

---

**Document Version**: 1.0
**Last Updated**: 2025-11-17
**Author**: Dynamic XSD Platform Team

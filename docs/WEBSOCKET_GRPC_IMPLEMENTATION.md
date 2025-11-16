# WebSocket & gRPC Implementation Plan

## Overview
This document outlines the implementation plan for adding WebSocket and gRPC protocol support to the Dynamic XSD Service Generation Platform.

## Current Status
- ✅ REST API support (fully implemented)
- ✅ SOAP support (fully implemented)
- 🚧 WebSocket support (in progress)
- ⏳ gRPC support (planned)

---

## Phase 1: WebSocket Implementation (Current)

### 1.1 Backend Components

#### Dependencies Added
- `spring-boot-starter-websocket`
- `spring-messaging`

#### Configuration
- **WebSocketConfig.java** - STOMP over WebSocket configuration
  - Endpoints: `/ws` (with and without SockJS)
  - Brokers: `/topic` (broadcast), `/queue` (point-to-point)
  - Application prefix: `/app`

#### Services to Create
1. **WebSocketServiceGenerator.java**
   - Generate WebSocket handlers from XSD schemas
   - Create message routing for each service
   - Setup topics: `/topic/{serviceName}/{operation}`

2. **DynamicWebSocketController.java**
   - Handle incoming WebSocket messages
   - Validate against XSD schemas
   - Route to appropriate service methods
   - Broadcast responses

3. **WebSocketSessionManager.java**
   - Track active connections
   - Manage subscriptions
   - Handle reconnection logic

#### Database Schema Updates
```sql
-- Add WebSocket support columns
ALTER TABLE service_definition
ADD COLUMN websocket_enabled BOOLEAN DEFAULT false;

-- Track WebSocket subscriptions
CREATE TABLE websocket_subscription (
    id VARCHAR(255) PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    topic VARCHAR(500) NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP,
    FOREIGN KEY (service_name) REFERENCES service_definition(service_name)
);

CREATE INDEX idx_ws_sub_service ON websocket_subscription(service_name);
CREATE INDEX idx_ws_sub_session ON websocket_subscription(session_id);
```

#### Message Protocol
```json
// Request Message
{
  "service": "customerservice",
  "operation": "CREATE|GET|UPDATE|DELETE|SUBSCRIBE",
  "messageId": "uuid",
  "data": {},
  "timestamp": 1234567890
}

// Response Message
{
  "messageId": "uuid",
  "success": true,
  "data": {},
  "error": null,
  "timestamp": 1234567890
}

// Event Broadcast
{
  "event": "CREATED|UPDATED|DELETED",
  "service": "customerservice",
  "data": {},
  "timestamp": 1234567890
}
```

### 1.2 Frontend Components

#### WebSocket Client
```typescript
// WebSocket service client
class WebSocketClient {
  connect(serviceName: string): void
  subscribe(topic: string, callback: Function): void
  send(message: WebSocketMessage): void
  disconnect(): void
}
```

#### UI Updates
- Add WebSocket toggle in deployment modal
- Show WebSocket status indicator
- Display active connections count
- Real-time event log viewer

### 1.3 Testing Strategy
- Unit tests for message validation
- Integration tests for pub/sub
- Load testing for concurrent connections
- Browser compatibility testing

---

## Phase 2: gRPC Implementation (Planned)

### 2.1 Technical Challenges

#### XSD to Proto Conversion
- Complex type mapping
- Namespace handling
- Import resolution
- Streaming patterns

#### Dynamic Compilation
- Runtime proto compilation
- ClassLoader management
- Service registration

### 2.2 Backend Components

#### Dependencies to Add
```xml
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>2.15.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.58.0</version>
</dependency>
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>3.24.0</version>
</dependency>
```

#### Services to Create
1. **ProtoGeneratorService.java**
   - Parse XSD schema
   - Map XSD types to proto types
   - Generate `.proto` files
   - Handle nested types and imports

2. **GrpcServiceGenerator.java**
   - Compile proto files dynamically
   - Generate gRPC service stubs
   - Create service implementations
   - Register with gRPC server

3. **DynamicGrpcServiceRegistry.java**
   - Manage gRPC services
   - Handle service lifecycle
   - Support multiple versions

#### Type Mapping
| XSD Type | Proto Type |
|----------|-----------|
| xs:string | string |
| xs:int | int32 |
| xs:long | int64 |
| xs:float | float |
| xs:double | double |
| xs:boolean | bool |
| xs:dateTime | google.protobuf.Timestamp |
| xs:decimal | string or custom |
| complexType | message |

#### Database Schema Updates
```sql
ALTER TABLE service_definition
ADD COLUMN grpc_endpoints INTEGER DEFAULT 0;

ALTER TABLE service_definition
ADD COLUMN grpc_port INTEGER;

ALTER TABLE service_endpoint
ADD COLUMN grpc_method_type VARCHAR(50); -- UNARY, SERVER_STREAMING, CLIENT_STREAMING, BIDI_STREAMING
```

### 2.3 Configuration
```yaml
grpc:
  server:
    port: 9090
    max-inbound-message-size: 10MB
  client:
    GLOBAL:
      negotiationType: plaintext
```

---

## Implementation Timeline

### Week 1-2: WebSocket (Current)
- ✅ Day 1: Dependencies and configuration
- 📅 Day 2-3: WebSocketServiceGenerator
- 📅 Day 4-5: DynamicWebSocketController
- 📅 Day 6-7: Session management and testing
- 📅 Day 8-9: Frontend integration
- 📅 Day 10: End-to-end testing

### Week 3-5: gRPC
- 📅 Week 3: Proto generation from XSD
- 📅 Week 4: Dynamic gRPC service generation
- 📅 Week 5: Testing and integration

---

## Protocol Comparison

| Feature | REST | SOAP | WebSocket | gRPC |
|---------|------|------|-----------|------|
| Latency | ~50ms | ~80ms | ~5ms | ~10ms |
| Throughput | Medium | Low | High | Very High |
| Real-time | ❌ | ❌ | ✅ | ✅ |
| Streaming | ❌ | ❌ | ✅ | ✅ |
| Browser Support | ✅ | ✅ | ✅ | ⚠️ |
| Type Safety | ⚠️ | ✅ | ❌ | ✅ |
| Overhead | Low | High | Low | Very Low |

---

## Use Cases

### WebSocket Best For
- Real-time dashboards
- Live notifications
- Chat/messaging
- Collaborative editing
- Event streaming
- IoT data feeds

### gRPC Best For
- Microservice communication
- High-performance APIs
- Bidirectional streaming
- Multi-language clients
- Internal services
- Mobile backends

---

## Security Considerations

### WebSocket
- ✅ Authentication via JWT in handshake
- ✅ Message-level authorization
- ✅ Rate limiting per connection
- ✅ Input validation against XSD

### gRPC
- ✅ TLS/SSL encryption
- ✅ Token-based authentication
- ✅ Interceptor-based authorization
- ✅ Message validation

---

## Monitoring & Metrics

### WebSocket Metrics
- Active connections count
- Messages sent/received
- Subscription count
- Connection duration
- Error rate

### gRPC Metrics
- Request rate
- Latency percentiles
- Error rate by method
- Stream duration
- Message size

---

## Next Steps

1. ✅ Complete WebSocket configuration
2. 📅 Implement WebSocketServiceGenerator
3. 📅 Create DynamicWebSocketController
4. 📅 Update database schema
5. 📅 Frontend WebSocket client
6. 📅 Testing and documentation
7. 📅 Start gRPC implementation

---

**Document Version**: 1.0
**Last Updated**: 2025-11-17
**Status**: WebSocket Phase 1 In Progress

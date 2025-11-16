# Protocol Testing Guide

Complete guide for testing GraphQL, ActiveMQ, and SFTP protocols.

---

## Prerequisites

✅ Backend running on `http://localhost:8080`
✅ Frontend running on `http://localhost:3000`
✅ PostgreSQL database connected
✅ All services healthy

---

## Test 1: GraphQL Protocol

### Step 1: Create a Sample XSD Schema

First, let's create a simple Customer XSD schema to test with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           targetNamespace="http://example.com/customer"
           elementFormDefault="qualified">

    <xs:element name="Customer">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="customerId" type="xs:string"/>
                <xs:element name="name" type="xs:string"/>
                <xs:element name="email" type="xs:string" minOccurs="0"/>
                <xs:element name="phone" type="xs:string" minOccurs="0"/>
                <xs:element name="createdAt" type="xs:dateTime" minOccurs="0"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

</xs:schema>
```

### Step 2: Upload XSD via REST API

```bash
# Save the XSD to a file
cat > customer.xsd <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           targetNamespace="http://example.com/customer"
           elementFormDefault="qualified">
    <xs:element name="Customer">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="customerId" type="xs:string"/>
                <xs:element name="name" type="xs:string"/>
                <xs:element name="email" type="xs:string" minOccurs="0"/>
                <xs:element name="phone" type="xs:string" minOccurs="0"/>
                <xs:element name="createdAt" type="xs:dateTime" minOccurs="0"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>
</xs:schema>
EOF

# Upload via REST API
curl -X POST http://localhost:8080/api/services \
  -H "Content-Type: multipart/form-data" \
  -F "serviceName=customerservice" \
  -F "targetNamespace=http://example.com/customer" \
  -F "file=@customer.xsd"
```

### Step 3: Test GraphQL Queries

**Option A: Using GraphiQL (Browser)**

1. Open `http://localhost:8080/graphiql` in your browser
2. Try the following queries:

**Query - Get Customer**
```graphql
query GetCustomer {
  customer(id: "1") {
    id
    customerId
    name
    email
    phone
    createdAt
  }
}
```

**Mutation - Create Customer**
```graphql
mutation CreateCustomer {
  createCustomer(input: {
    customerId: "CUST001"
    name: "John Doe"
    email: "john@example.com"
    phone: "+1234567890"
  }) {
    id
    customerId
    name
    email
  }
}
```

**Query - List Customers**
```graphql
query ListCustomers {
  customers(page: 0, pageSize: 10) {
    id
    name
    email
  }
}
```

**Mutation - Update Customer**
```graphql
mutation UpdateCustomer {
  updateCustomer(id: "1", input: {
    customerId: "CUST001"
    name: "Jane Doe"
    email: "jane@example.com"
  }) {
    id
    name
    email
  }
}
```

**Mutation - Delete Customer**
```graphql
mutation DeleteCustomer {
  deleteCustomer(id: "1") {
    success
    message
  }
}
```

**Subscription - Watch Customer Changes**
```graphql
subscription WatchCustomerChanges {
  customerChanges {
    eventType
    entityId
    timestamp
  }
}
```

**Option B: Using cURL**

```bash
# Create Customer
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { createCustomer(input: { customerId: \"CUST001\", name: \"John Doe\", email: \"john@example.com\" }) { id customerId name email } }"
  }'

# Get Customer
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { customer(id: \"1\") { id name email } }"
  }'

# List Customers
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { customers(page: 0, pageSize: 10) { id name email } }"
  }'
```

### Expected GraphQL Schema

The platform should generate this schema:

```graphql
scalar DateTime
scalar JSON
scalar Long

type Customer {
  id: ID!
  customerId: String!
  name: String!
  email: String
  phone: String
  createdAt: DateTime
}

input CustomerInput {
  customerId: String!
  name: String!
  email: String
  phone: String
}

type Query {
  customer(id: ID!): Customer
  customers(page: Int, pageSize: Int): [Customer!]!
  searchCustomer(filter: JSON): [Customer!]!
}

type Mutation {
  createCustomer(input: CustomerInput!): Customer!
  updateCustomer(id: ID!, input: CustomerInput!): Customer!
  deleteCustomer(id: ID!): DeleteResponse!
}

type Subscription {
  customerUpdated(id: ID): Customer!
  customerChanges: ChangeEvent!
}
```

---

## Test 2: ActiveMQ Protocol

### Step 1: Verify ActiveMQ is Running

```bash
# Check if ActiveMQ broker is initialized
docker logs xsd-platform-backend 2>&1 | grep -i "activemq\|jms"

# Access ActiveMQ Web Console (if external broker)
# http://localhost:8161/admin
# Credentials: admin/admin
```

### Step 2: Send Messages to Queue

**Using Java (Spring Boot)**

Create a test class:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/activemq")
public class ActiveMQTestController {

    @Autowired
    private JmsTemplate jmsTemplate;

    @PostMapping("/send")
    public String sendMessage(@RequestBody Map<String, Object> payload) {
        Map<String, Object> message = Map.of(
            "service", "customerservice",
            "operation", "CREATE",
            "data", payload,
            "timestamp", System.currentTimeMillis()
        );

        jmsTemplate.convertAndSend("service.customerservice", message);
        return "Message sent to queue: service.customerservice";
    }
}
```

**Using cURL (via REST endpoint)**

First, create a REST endpoint to send JMS messages, then:

```bash
# Send CREATE message
curl -X POST http://localhost:8080/test/activemq/send \
  -H "Content-Type: application/json" \
  -d '{
    "service": "customerservice",
    "operation": "CREATE",
    "data": {
      "customerId": "CUST002",
      "name": "Alice Smith",
      "email": "alice@example.com"
    }
  }'
```

### Step 3: Subscribe to Topics

**Java Listener Example**:

```java
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class CustomerEventListener {

    @JmsListener(
        destination = "events.customerservice",
        containerFactory = "jmsTopicListenerContainerFactory"
    )
    public void onCustomerEvent(Map<String, Object> event) {
        System.out.println("Received event: " + event.get("eventType"));
        System.out.println("Data: " + event.get("data"));
    }
}
```

### Step 4: Test Queue Operations

**Send Messages to Queue**:

```bash
# CREATE operation
curl -X POST http://localhost:8080/api/activemq/customerservice/send \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "CREATE",
    "data": {
      "customerId": "CUST003",
      "name": "Bob Johnson",
      "email": "bob@example.com"
    }
  }'

# GET operation
curl -X POST http://localhost:8080/api/activemq/customerservice/send \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "GET",
    "data": {
      "id": "1"
    }
  }'

# LIST operation
curl -X POST http://localhost:8080/api/activemq/customerservice/send \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "LIST",
    "data": {}
  }'
```

### Message Format

**Request Message** (Queue: `service.customerservice`):
```json
{
  "service": "customerservice",
  "operation": "CREATE",
  "data": {
    "customerId": "CUST001",
    "name": "John Doe",
    "email": "john@example.com"
  },
  "timestamp": 1700000000000
}
```

**Event Message** (Topic: `events.customerservice`):
```json
{
  "service": "customerservice",
  "eventType": "CREATED",
  "data": {
    "id": "1",
    "customerId": "CUST001",
    "name": "John Doe",
    "email": "john@example.com"
  },
  "timestamp": 1700000001000
}
```

---

## Test 3: SFTP Protocol

### Step 1: Initialize SFTP Directories

```bash
# Create SFTP directory structure
mkdir -p /sftp/customerservice/{requests,responses,errors,archive}

# Or use the service API
curl -X POST http://localhost:8080/api/sftp/customerservice/init
```

### Step 2: Create Request Files

**CREATE Request**:

```bash
# Create request file
cat > /sftp/customerservice/requests/$(date +%s)_create_request.json <<'EOF'
{
  "service": "customerservice",
  "operation": "CREATE",
  "data": {
    "customerId": "CUST004",
    "name": "Charlie Brown",
    "email": "charlie@example.com",
    "phone": "+9876543210"
  },
  "timestamp": 1700000000000,
  "requestId": "req-001"
}
EOF
```

**GET Request**:

```bash
cat > /sftp/customerservice/requests/$(date +%s)_get_request.json <<'EOF'
{
  "service": "customerservice",
  "operation": "GET",
  "data": {
    "id": "1"
  },
  "timestamp": 1700000100000,
  "requestId": "req-002"
}
EOF
```

**LIST Request**:

```bash
cat > /sftp/customerservice/requests/$(date +%s)_list_request.json <<'EOF'
{
  "service": "customerservice",
  "operation": "LIST",
  "data": {},
  "timestamp": 1700000200000,
  "requestId": "req-003"
}
EOF
```

**UPDATE Request**:

```bash
cat > /sftp/customerservice/requests/$(date +%s)_update_request.json <<'EOF'
{
  "service": "customerservice",
  "operation": "UPDATE",
  "data": {
    "id": "1",
    "customerId": "CUST004",
    "name": "Charlie Brown Jr.",
    "email": "charlie.jr@example.com"
  },
  "timestamp": 1700000300000,
  "requestId": "req-004"
}
EOF
```

**DELETE Request**:

```bash
cat > /sftp/customerservice/requests/$(date +%s)_delete_request.json <<'EOF'
{
  "service": "customerservice",
  "operation": "DELETE",
  "data": {
    "id": "1"
  },
  "timestamp": 1700000400000,
  "requestId": "req-005"
}
EOF
```

### Step 3: Trigger File Processing

```bash
# Trigger manual processing (if not auto-polling)
curl -X POST http://localhost:8080/api/sftp/customerservice/process
```

### Step 4: Check Response Files

```bash
# List response files
ls -lh /sftp/customerservice/responses/

# View response
cat /sftp/customerservice/responses/req-001_response.json
```

**Expected Response Format**:

```json
{
  "requestId": "req-001",
  "service": "customerservice",
  "success": true,
  "data": {
    "id": "1",
    "customerId": "CUST004",
    "name": "Charlie Brown",
    "email": "charlie@example.com",
    "phone": "+9876543210",
    "createdAt": 1700000000000
  },
  "timestamp": 1700000001000
}
```

### Step 5: Check Archived Files

```bash
# Files are automatically moved to archive after processing
ls -lh /sftp/customerservice/archive/

# View archived request
cat /sftp/customerservice/archive/1700000000_*_create_request.json
```

### Step 6: Check Error Files (if any)

```bash
# List error files
ls -lh /sftp/customerservice/errors/

# View error
cat /sftp/customerservice/errors/req-001_error.json
```

**Error Format**:

```json
{
  "requestId": "req-001",
  "service": "customerservice",
  "success": false,
  "error": "Customer ID already exists",
  "timestamp": 1700000001000
}
```

### Step 7: Monitor Statistics

```bash
# Get SFTP statistics
curl -X GET http://localhost:8080/api/sftp/customerservice/stats

# Expected output:
# {
#   "requestsPending": 0,
#   "responsesGenerated": 5,
#   "errors": 0,
#   "archived": 5
# }
```

---

## Complete End-to-End Test Script

Create a file `test-all-protocols.sh`:

```bash
#!/bin/bash

echo "========================================="
echo "Testing All 7 Protocols"
echo "========================================="

# Step 1: Upload XSD Schema
echo -e "\n1. Uploading Customer XSD Schema..."
cat > /tmp/customer.xsd <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           targetNamespace="http://example.com/customer"
           elementFormDefault="qualified">
    <xs:element name="Customer">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="customerId" type="xs:string"/>
                <xs:element name="name" type="xs:string"/>
                <xs:element name="email" type="xs:string" minOccurs="0"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>
</xs:schema>
EOF

curl -X POST http://localhost:8080/api/services \
  -F "serviceName=customerservice" \
  -F "targetNamespace=http://example.com/customer" \
  -F "file=@/tmp/customer.xsd"

sleep 2

# Step 2: Test REST API
echo -e "\n2. Testing REST API..."
curl -X POST http://localhost:8080/api/dynamic/customerservice \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST001","name":"REST User","email":"rest@example.com"}'

# Step 3: Test GraphQL
echo -e "\n3. Testing GraphQL..."
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation{createCustomer(input:{customerId:\"CUST002\",name:\"GraphQL User\"}){id name}}"}'

# Step 4: Test ActiveMQ (requires endpoint)
echo -e "\n4. Testing ActiveMQ..."
# (Implementation depends on your ActiveMQ endpoint)

# Step 5: Test SFTP
echo -e "\n5. Testing SFTP..."
mkdir -p /sftp/customerservice/requests
cat > /sftp/customerservice/requests/$(date +%s)_test.json <<'EOF'
{
  "service": "customerservice",
  "operation": "CREATE",
  "data": {"customerId":"CUST003","name":"SFTP User"},
  "requestId": "test-001"
}
EOF

echo -e "\n========================================="
echo "All tests completed!"
echo "========================================="
```

Run it:

```bash
chmod +x test-all-protocols.sh
./test-all-protocols.sh
```

---

## Verification Checklist

After running tests, verify:

### GraphQL
- [ ] GraphiQL is accessible at `http://localhost:8080/graphiql`
- [ ] Schema is generated correctly
- [ ] Queries return data
- [ ] Mutations create/update/delete entities
- [ ] Subscriptions work (if WebSocket connected)

### ActiveMQ
- [ ] Messages are sent to queues
- [ ] Events are published to topics
- [ ] Listeners receive messages
- [ ] CRUD operations work via JMS

### SFTP
- [ ] Directories are created automatically
- [ ] Request files are processed
- [ ] Response files are generated
- [ ] Requests are archived after processing
- [ ] Error files are created for failures

---

## Troubleshooting

### GraphQL 404/500
- Check if service is deployed: `curl http://localhost:8080/api/services`
- Verify GraphQL endpoint: `curl http://localhost:8080/graphql`
- Check logs: `docker logs xsd-platform-backend | grep GraphQL`

### ActiveMQ Connection Issues
- Verify broker URL in application.yml
- Check if embedded broker started: `docker logs xsd-platform-backend | grep ActiveMQ`
- Test with ActiveMQ console (if using external broker)

### SFTP File Not Processing
- Check directory permissions: `ls -lh /sftp/customerservice/`
- Verify file format is valid JSON
- Check logs: `docker logs xsd-platform-backend | grep SFTP`
- Manually trigger processing if auto-polling disabled

---

## Performance Testing

### Load Test GraphQL

```bash
# Install Apache Bench
# Run 1000 requests with 10 concurrent connections
ab -n 1000 -c 10 -p graphql-query.json -T 'application/json' \
  http://localhost:8080/graphql
```

### Load Test ActiveMQ

```bash
# Send 1000 messages
for i in {1..1000}; do
  curl -X POST http://localhost:8080/api/activemq/send \
    -H "Content-Type: application/json" \
    -d "{\"data\":{\"id\":$i}}" &
done
wait
```

### Batch Test SFTP

```bash
# Create 100 request files
for i in {1..100}; do
  cat > /sftp/customerservice/requests/$(date +%s%N)_request.json <<EOF
{
  "service": "customerservice",
  "operation": "CREATE",
  "data": {"customerId":"CUST$i","name":"User $i"},
  "requestId": "batch-$i"
}
EOF
  sleep 0.01
done

# Process all
curl -X POST http://localhost:8080/api/sftp/customerservice/process
```

---

## Next Steps

1. **Monitor Logs**: Watch real-time logs for all protocols
   ```bash
   docker logs -f xsd-platform-backend
   ```

2. **Check Metrics**: View application metrics
   ```bash
   curl http://localhost:8080/actuator/metrics
   ```

3. **Production Deployment**: Use guides in documentation for production setup

---

**Status**: Ready for Testing
**Protocols**: GraphQL, ActiveMQ, SFTP
**Date**: 2025-11-17

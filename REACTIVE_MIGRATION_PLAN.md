# Reactive Stack Migration Plan

## Executive Summary

**Migration Goal:** Convert the Dynamic XSD Service Generation Platform from blocking (Spring Web + JPA) to fully reactive architecture (WebFlux + R2DBC) to meet specification requirements.

**Current Status:** Migration Started - Dependencies Updated
**Estimated Effort:** 3-4 weeks (1 developer)
**Completion:** ~5%

---

## Table of Contents

1. [Current Architecture](#current-architecture)
2. [Target Architecture](#target-architecture)
3. [Migration Phases](#migration-phases)
4. [Completed Work](#completed-work)
5. [Pending Work](#pending-work)
6. [Critical Decisions](#critical-decisions)
7. [Technical Challenges](#technical-challenges)
8. [Testing Strategy](#testing-strategy)
9. [Rollback Plan](#rollback-plan)

---

## Current Architecture

### Technology Stack (Before Migration)

| Layer | Technology | Type |
|-------|-----------|------|
| **Web Framework** | Spring Web MVC | Blocking |
| **Database** | Spring Data JPA + Hibernate | Blocking |
| **Database Driver** | PostgreSQL JDBC | Blocking |
| **Security** | Spring Security (Servlet) | Blocking |
| **API Docs** | Springdoc WebMVC | Blocking |
| **SOAP** | Apache CXF | Blocking |

### Architecture Diagram (Current - Blocking)

```
┌─────────────────────────────────────────────────┐
│          HTTP Request (Thread-per-Request)      │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│         Spring Web MVC Controller               │
│              (Blocking Thread)                  │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│           Service Layer                         │
│         (Blocking Operations)                   │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│      JPA Repository (Blocking)                  │
│      Hibernate ORM (Blocking)                   │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│      PostgreSQL JDBC Driver (Blocking)          │
│      Connection Pool (HikariCP)                 │
└─────────────────────────────────────────────────┘
```

**Performance Characteristics:**
- **Thread Model:** Thread-per-request (200-300 concurrent requests max)
- **Database Connections:** Limited by connection pool (default 10)
- **Latency:** Threads blocked during I/O operations
- **Scalability:** Vertical scaling (more CPU/RAM)

---

## Target Architecture

### Technology Stack (After Migration)

| Layer | Technology | Type |
|-------|-----------|------|
| **Web Framework** | Spring WebFlux | Reactive |
| **Database** | Spring Data R2DBC | Reactive |
| **Database Driver** | R2DBC PostgreSQL | Reactive |
| **Security** | Spring Security Reactive | Reactive |
| **API Docs** | Springdoc WebFlux | Reactive |
| **SOAP** | Spring WS (Reactive Adapter) | Hybrid |

### Architecture Diagram (Target - Reactive)

```
┌─────────────────────────────────────────────────┐
│        HTTP Request (Event Loop - Netty)        │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│       Spring WebFlux Handler/Router             │
│         (Non-blocking Event Loop)               │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│           Service Layer                         │
│       Returns Mono<T> / Flux<T>                 │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│    R2DBC Repository (Reactive)                  │
│    Returns Mono<T> / Flux<T>                    │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│    R2DBC PostgreSQL Driver (Reactive)           │
│    Non-blocking I/O (Connection Pool)           │
└─────────────────────────────────────────────────┘
```

**Performance Characteristics:**
- **Thread Model:** Event loop (1000s of concurrent requests)
- **Database Connections:** Reactive connection pool (efficient)
- **Latency:** Non-blocking I/O operations
- **Scalability:** Horizontal scaling (more instances)

---

## Migration Phases

### Phase 1: Dependencies & Configuration ✅ STARTED (5% Complete)

**Objective:** Update Maven dependencies to reactive stack

**Tasks:**
- [x] Remove `spring-boot-starter-web` (blocking)
- [x] Keep `spring-boot-starter-webflux` (reactive)
- [x] Remove `spring-boot-starter-data-jpa` (blocking)
- [x] Keep `spring-boot-starter-data-r2dbc` (reactive)
- [x] Update `springdoc-openapi-starter-webmvc-ui` → `springdoc-openapi-starter-webflux-ui`
- [ ] Remove Hibernate dependencies
- [ ] Add R2DBC connection pool
- [ ] Update application.yml for R2DBC configuration

**Status:** 60% Complete

---

### Phase 2: Database Layer Migration ⏸️ NOT STARTED (0% Complete)

**Objective:** Migrate JPA entities and repositories to R2DBC

#### 2.1 Entity Migration

**From (JPA):**
```java
@Entity
@Table(name = "schema_metadata")
public class SchemaMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String serviceName;

    @OneToMany(mappedBy = "schema")
    private List<ServiceDefinition> services;
}
```

**To (R2DBC):**
```java
@Table("schema_metadata")
public class SchemaMetadata {
    @Id
    private UUID id;

    private String serviceName;

    @Transient  // R2DBC doesn't support relationships
    private List<ServiceDefinition> services;
}
```

**Entities to Migrate:**
- [ ] `SchemaMetadata`
- [ ] `ServiceDefinition`
- [ ] `EndpointMapping`
- [ ] `User`
- [ ] `AuditLog`

**Key Changes:**
- Remove `@Entity`, `@Table(name = "...")` → `@Table("...")`
- Remove `@GeneratedValue` → Manual ID generation or database sequence
- Remove `@OneToMany`, `@ManyToOne` → R2DBC doesn't support relationships
- Handle relationships manually in service layer

#### 2.2 Repository Migration

**From (JPA):**
```java
@Repository
public interface SchemaMetadataRepository extends JpaRepository<SchemaMetadata, UUID> {
    Optional<SchemaMetadata> findByServiceName(String serviceName);

    @Query("SELECT s FROM SchemaMetadata s WHERE s.status = :status")
    List<SchemaMetadata> findByStatus(@Param("status") SchemaStatus status);
}
```

**To (R2DBC):**
```java
@Repository
public interface SchemaMetadataRepository extends ReactiveCrudRepository<SchemaMetadata, UUID> {
    Mono<SchemaMetadata> findByServiceName(String serviceName);

    @Query("SELECT * FROM schema_metadata WHERE status = :status")
    Flux<SchemaMetadata> findByStatus(@Param("status") String status);
}
```

**Repositories to Migrate:**
- [ ] `SchemaMetadataRepository`
- [ ] `ServiceDefinitionRepository`
- [ ] `EndpointMappingRepository`
- [ ] `UserRepository`
- [ ] `AuditLogRepository`

**Key Changes:**
- Extend `ReactiveCrudRepository` instead of `JpaRepository`
- Return `Mono<T>` instead of `Optional<T>`
- Return `Flux<T>` instead of `List<T>`
- Return `Mono<Void>` instead of `void`
- Use SQL queries instead of JPQL in `@Query`

---

### Phase 3: Service Layer Migration ⏸️ NOT STARTED (0% Complete)

**Objective:** Refactor services to return reactive types (Mono/Flux)

#### 3.1 Service Method Signatures

**From (Blocking):**
```java
@Service
@RequiredArgsConstructor
public class SchemaManagementService {

    public SchemaMetadataDto uploadSchema(MultipartFile file, String serviceName) {
        // Blocking code
        SchemaMetadata schema = schemaRepository.save(metadata);
        return toDto(schema);
    }

    public List<SchemaMetadataDto> listSchemas(SchemaStatus status) {
        return schemaRepository.findByStatus(status).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}
```

**To (Reactive):**
```java
@Service
@RequiredArgsConstructor
public class SchemaManagementService {

    public Mono<SchemaMetadataDto> uploadSchema(FilePart filePart, String serviceName) {
        // Reactive code
        return filePart.content()
            .reduce(DataBufferUtils::retain)
            .flatMap(dataBuffer -> {
                // Process XSD
                return schemaRepository.save(metadata);
            })
            .map(this::toDto);
    }

    public Flux<SchemaMetadataDto> listSchemas(SchemaStatus status) {
        return schemaRepository.findByStatus(status.name())
            .map(this::toDto);
    }
}
```

**Services to Migrate:**
- [ ] `SchemaManagementService`
- [ ] `ServiceDeploymentService`
- [ ] `XsdValidatorService`
- [ ] `PojoGeneratorService`
- [ ] `DynamicCompilerService`
- [ ] `AuthenticationService`
- [ ] `UserService`

**Key Patterns:**
- File upload: `MultipartFile` → `FilePart`
- Blocking I/O → `Mono.fromCallable(() -> blockingOperation()).subscribeOn(Schedulers.boundedElastic())`
- Transactions: `@Transactional` → `TransactionalOperator`
- Error handling: `.onErrorResume()`, `.onErrorReturn()`, `.doOnError()`

---

### Phase 4: Controller Layer Migration ⏸️ NOT STARTED (0% Complete)

**Objective:** Update controllers to handle reactive streams

#### 4.1 Controller Migration

**From (Blocking):**
```java
@RestController
@RequestMapping("/api/v1/schema")
public class SchemaManagementController {

    @PostMapping("/upload")
    public ResponseEntity<SchemaUploadResponse> uploadSchema(
        @RequestPart("file") MultipartFile file,
        @RequestParam String serviceName
    ) {
        SchemaMetadataDto result = schemaService.uploadSchema(file, serviceName);
        return ResponseEntity.ok(new SchemaUploadResponse(result));
    }

    @GetMapping("/list")
    public ResponseEntity<List<SchemaMetadataDto>> listSchemas(
        @RequestParam(required = false) SchemaStatus status
    ) {
        List<SchemaMetadataDto> schemas = schemaService.listSchemas(status);
        return ResponseEntity.ok(schemas);
    }
}
```

**To (Reactive):**
```java
@RestController
@RequestMapping("/api/v1/schema")
public class SchemaManagementController {

    @PostMapping("/upload")
    public Mono<ResponseEntity<SchemaUploadResponse>> uploadSchema(
        @RequestPart("file") Mono<FilePart> filePart,
        @RequestParam String serviceName
    ) {
        return filePart
            .flatMap(file -> schemaService.uploadSchema(file, serviceName))
            .map(result -> ResponseEntity.ok(new SchemaUploadResponse(result)));
    }

    @GetMapping("/list")
    public Mono<ResponseEntity<List<SchemaMetadataDto>>> listSchemas(
        @RequestParam(required = false) SchemaStatus status
    ) {
        return schemaService.listSchemas(status)
            .collectList()
            .map(ResponseEntity::ok);
    }
}
```

**Controllers to Migrate:**
- [ ] `SchemaManagementController`
- [ ] `ServiceManagementController`
- [ ] `DashboardController`
- [ ] `DynamicServiceController`
- [ ] `AuthController`
- [ ] `UserController`

**Key Changes:**
- Return `Mono<ResponseEntity<T>>` or `Flux<T>`
- File upload: `MultipartFile` → `Mono<FilePart>`
- Pagination: Use `PageRequest` with reactive repository methods
- Error handling: WebFlux error handlers

---

### Phase 5: Security Migration ⏸️ NOT STARTED (0% Complete)

**Objective:** Migrate Spring Security to reactive security

#### 5.1 Security Configuration

**From (Blocking):**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**To (Reactive):**
```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/v1/auth/**").permitAll()
                .anyExchange().authenticated()
            )
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }
}
```

**Components to Migrate:**
- [ ] `SecurityConfig`
- [ ] `JwtAuthenticationFilter` → Reactive WebFilter
- [ ] `CustomUserDetailsService` → `ReactiveUserDetailsService`
- [ ] `JwtTokenProvider` (mostly unchanged)

**Key Changes:**
- `@EnableWebSecurity` → `@EnableWebFluxSecurity`
- `SecurityFilterChain` → `SecurityWebFilterChain`
- `HttpSecurity` → `ServerHttpSecurity`
- `UserDetailsService` → `ReactiveUserDetailsService`
- `OncePerRequestFilter` → `WebFilter`

---

### Phase 6: Configuration Updates ⏸️ NOT STARTED (0% Complete)

**Objective:** Update application.yml for reactive stack

#### 6.1 Database Configuration

**From (JPA):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/xsdplatform
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: true
```

**To (R2DBC):**
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/xsdplatform
    username: postgres
    password: postgres
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m

  # Database initialization
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
```

#### 6.2 Server Configuration

**Update:**
```yaml
server:
  # Netty server (WebFlux default)
  port: 8080
  netty:
    connection-timeout: 30s
```

---

### Phase 7: Testing ⏸️ NOT STARTED (0% Complete)

**Objective:** Create comprehensive test suite for reactive code

#### 7.1 Repository Tests

**Example:**
```java
@DataR2dbcTest
class SchemaMetadataRepositoryTest {

    @Autowired
    private SchemaMetadataRepository repository;

    @Test
    void shouldSaveAndFindByServiceName() {
        SchemaMetadata schema = new SchemaMetadata();
        schema.setServiceName("test-service");

        StepVerifier.create(
            repository.save(schema)
                .flatMap(saved -> repository.findByServiceName("test-service"))
        )
        .assertNext(found -> {
            assertThat(found.getServiceName()).isEqualTo("test-service");
        })
        .verifyComplete();
    }
}
```

#### 7.2 Service Tests

**Example:**
```java
@ExtendWith(MockitoExtension.class)
class SchemaManagementServiceTest {

    @Mock
    private SchemaMetadataRepository repository;

    @InjectMocks
    private SchemaManagementService service;

    @Test
    void shouldUploadSchema() {
        // Use StepVerifier for reactive tests
        StepVerifier.create(service.uploadSchema(filePart, "test-service"))
            .assertNext(result -> {
                assertThat(result.getServiceName()).isEqualTo("test-service");
            })
            .verifyComplete();
    }
}
```

#### 7.3 Controller Tests

**Example:**
```java
@WebFluxTest(SchemaManagementController.class)
class SchemaManagementControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private SchemaManagementService service;

    @Test
    void shouldListSchemas() {
        when(service.listSchemas(any()))
            .thenReturn(Flux.just(new SchemaMetadataDto()));

        webTestClient.get()
            .uri("/api/v1/schema/list")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(SchemaMetadataDto.class)
            .hasSize(1);
    }
}
```

**Tests to Create:**
- [ ] Repository tests (StepVerifier)
- [ ] Service tests (Mockito + StepVerifier)
- [ ] Controller tests (WebTestClient)
- [ ] Integration tests (TestContainers + R2DBC)
- [ ] Performance tests (reactive load testing)

---

### Phase 8: SOAP Service Handling 🚧 DECISION REQUIRED

**Objective:** Determine approach for SOAP services in reactive context

#### Option 1: Hybrid Architecture (Recommended)
- Keep Apache CXF with embedded Tomcat for SOAP
- Use WebFlux for REST APIs
- Run both on different ports

**Pros:**
- Minimal changes to existing SOAP code
- CXF is mature and well-tested
- Clear separation of concerns

**Cons:**
- Not fully reactive
- Requires two server runtimes
- More complex deployment

#### Option 2: Spring WS with WebFlux
- Replace CXF with Spring Web Services
- Use WebFlux adapters for SOAP

**Pros:**
- Fully reactive stack
- Single runtime (Netty)
- Consistent architecture

**Cons:**
- Requires rewriting all SOAP generation logic
- Spring WS less feature-rich than CXF
- More migration effort (2-3 weeks)

#### Option 3: REST Only
- Remove SOAP support entirely
- Focus on REST APIs

**Pros:**
- Simplest migration
- Fully reactive
- Modern API approach

**Cons:**
- Doesn't meet specification
- Loses SOAP functionality

**Decision Needed:** Which option to pursue?

---

## Completed Work

### ✅ Phase 1 Progress (60% Complete)

**Completed:**
1. Removed `spring-boot-starter-web` (blocking)
2. Removed `spring-boot-starter-data-jpa` (blocking)
3. Updated Springdoc to WebFlux version
4. Cleaned up duplicate dependencies

**Files Modified:**
- `backend/pom.xml` - Dependencies updated

**Remaining in Phase 1:**
- Remove Hibernate-specific dependencies
- Add R2DBC connection pool library
- Update `application.yml` configuration
- Update `application-dev.yml` configuration
- Update `application-prod.yml` configuration

---

## Pending Work

### Priority Order

#### 🔴 Critical Priority

1. **Update application.yml** - Configure R2DBC connection
2. **Migrate SchemaMetadata entity** - Core entity
3. **Migrate SchemaMetadataRepository** - Core repository
4. **Update SchemaManagementService** - Core service
5. **Update SchemaManagementController** - Core controller

#### 🟡 High Priority

6. Migrate remaining entities (User, ServiceDefinition, etc.)
7. Migrate remaining repositories
8. Update ServiceDeploymentService
9. Update authentication services
10. Migrate security configuration

#### 🟢 Medium Priority

11. Create reactive test suite
12. Update dynamic endpoint generation for reactive
13. Migrate remaining controllers
14. Performance testing

---

## Critical Decisions

### Decision 1: SOAP Service Strategy
**Status:** ⏸️ PENDING
**Options:** Hybrid, Spring WS, Remove SOAP
**Impact:** High - Affects 30% of codebase
**Deadline:** Before Phase 8

### Decision 2: Relationship Handling
**Status:** ⏸️ PENDING
**Question:** How to handle JPA relationships in R2DBC?
**Options:**
- Manual joins in service layer
- Separate queries + merge
- DatabaseClient for custom queries
**Impact:** Medium - Code complexity

### Decision 3: Transaction Management
**Status:** ⏸️ PENDING
**Question:** Transaction strategy in reactive context?
**Options:**
- `@Transactional` (limited support)
- `TransactionalOperator` (programmatic)
- Manual transaction boundaries
**Impact:** Medium - Data consistency

### Decision 4: Blocking Code Isolation
**Status:** ⏸️ PENDING
**Question:** How to handle blocking operations (file I/O, compilation)?
**Options:**
- Wrap in `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())`
- Separate thread pool
- Keep synchronous
**Impact:** High - Performance

---

## Technical Challenges

### Challenge 1: JPA → R2DBC Relationships
**Issue:** R2DBC doesn't support `@OneToMany`, `@ManyToOne`, etc.

**Solution:**
```java
// Instead of lazy-loaded relationships:
@Service
public class SchemaService {
    public Mono<SchemaWithServices> getSchemaWithServices(UUID id) {
        return schemaRepository.findById(id)
            .flatMap(schema ->
                serviceRepository.findBySchemaId(schema.getId())
                    .collectList()
                    .map(services -> new SchemaWithServices(schema, services))
            );
    }
}
```

### Challenge 2: File Upload Handling
**Issue:** `MultipartFile` is blocking

**Solution:**
```java
@PostMapping("/upload")
public Mono<ResponseEntity<Response>> upload(@RequestPart("file") Mono<FilePart> filePart) {
    return filePart
        .flatMap(file -> {
            // Get data buffer flux
            return file.content()
                .reduce(DataBuffer::write)
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return bytes;
                });
        })
        .flatMap(xsdBytes -> processXsd(xsdBytes));
}
```

### Challenge 3: Blocking JAXB Operations
**Issue:** JAXB XJC compilation is blocking

**Solution:**
```java
public Mono<List<String>> generatePojos(String xsdContent) {
    return Mono.fromCallable(() -> {
        // Blocking JAXB operations here
        return jaxbXjc.generate(xsdContent);
    }).subscribeOn(Schedulers.boundedElastic());
}
```

### Challenge 4: Transaction Management
**Issue:** R2DBC transactions work differently

**Solution:**
```java
@Service
public class ServiceWithTransactions {
    private final TransactionalOperator transactionalOperator;

    public Mono<Result> performTransaction() {
        return repository.save(entity1)
            .then(repository.save(entity2))
            .as(transactionalOperator::transactional);
    }
}
```

---

## Testing Strategy

### Unit Tests
- **Framework:** JUnit 5 + Mockito + StepVerifier
- **Coverage Target:** >80%
- **Focus:** Service layer logic

### Integration Tests
- **Framework:** TestContainers + R2DBC
- **Database:** PostgreSQL in container
- **Focus:** Repository + Service integration

### Controller Tests
- **Framework:** WebTestClient
- **Focus:** HTTP request/response handling

### Performance Tests
- **Framework:** Gatling or JMeter
- **Metrics:** Throughput, latency, resource usage
- **Comparison:** Blocking vs Reactive performance

---

## Rollback Plan

### If Migration Fails

**Option A: Revert to Blocking Stack**
1. Restore `pom.xml` from git history
2. Keep all entity/service code (unchanged if migration incomplete)
3. Re-enable JPA configuration
4. **Effort:** 1 day

**Option B: Partial Reactive**
1. Keep WebFlux for new endpoints
2. Use JPA for existing endpoints
3. Gradual migration over time
4. **Effort:** Ongoing

**Backup Strategy:**
- Git branch: `feature/reactive-migration`
- Tag before migration: `v1.0.0-pre-reactive`
- Keep blocking code in separate branch: `legacy/blocking-stack`

---

## Timeline & Milestones

### Week 1: Foundation
- ✅ Dependencies update
- ⏸️ Configuration update
- ⏸️ Entity migration (2 entities)
- ⏸️ Repository migration (2 repositories)

### Week 2: Core Services
- Service layer migration (SchemaManagement)
- Controller migration (SchemaManagementController)
- Basic testing setup

### Week 3: Remaining Services
- All service migrations
- All controller migrations
- Security migration

### Week 4: Testing & Polish
- Complete test suite
- Performance testing
- Bug fixes
- Documentation

---

## References

### Spring Documentation
- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)
- [Spring Security Reactive](https://docs.spring.io/spring-security/reference/reactive/index.html)

### Code Examples
- [Reactive CRUD Example](https://github.com/spring-projects/spring-data-examples/tree/main/r2dbc)
- [WebFlux Security Example](https://github.com/spring-projects/spring-security-samples/tree/main/servlet/java-configuration)

### Performance Guides
- [Reactor Performance Tuning](https://projectreactor.io/docs/core/release/reference/#performance)
- [R2DBC Connection Pooling](https://github.com/r2dbc/r2dbc-pool)

---

## Glossary

- **Mono<T>:** Reactive publisher of 0 or 1 element
- **Flux<T>:** Reactive publisher of 0 to N elements
- **R2DBC:** Reactive Relational Database Connectivity
- **WebFlux:** Spring's reactive web framework
- **Netty:** Non-blocking I/O server (WebFlux default)
- **StepVerifier:** Testing utility for reactive streams
- **Schedulers:** Thread pool management for reactive operations
- **Backpressure:** Flow control in reactive streams

---

## Status Dashboard

| Component | Status | Progress | Owner | ETA |
|-----------|--------|----------|-------|-----|
| **Dependencies** | 🟡 In Progress | 60% | - | Week 1 |
| **Configuration** | ⚪ Not Started | 0% | - | Week 1 |
| **Entities** | ⚪ Not Started | 0% | - | Week 1-2 |
| **Repositories** | ⚪ Not Started | 0% | - | Week 2 |
| **Services** | ⚪ Not Started | 0% | - | Week 2-3 |
| **Controllers** | ⚪ Not Started | 0% | - | Week 3 |
| **Security** | ⚪ Not Started | 0% | - | Week 3 |
| **Tests** | ⚪ Not Started | 0% | - | Week 4 |

**Legend:**
- 🟢 Complete
- 🟡 In Progress
- ⚪ Not Started
- 🔴 Blocked

---

## Next Steps

### Immediate Actions Required:

1. **Decision on SOAP Strategy** - Choose Option 1, 2, or 3
2. **Review Migration Plan** - Approve or modify approach
3. **Begin Phase 1 Completion:**
   - Update `application.yml` for R2DBC
   - Add R2DBC connection pool
   - Remove Hibernate dependencies

4. **Start Phase 2:**
   - Migrate `SchemaMetadata` entity
   - Migrate `SchemaMetadataRepository`
   - Create first reactive test

---

**Document Version:** 1.0
**Last Updated:** 2025-11-16
**Author:** Migration Team
**Status:** Active - Awaiting Decisions

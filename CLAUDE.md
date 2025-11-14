# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**InvernaderosAPI** is a production IoT greenhouse monitoring system that receives sensor data via MQTT, stores it in TimescaleDB/PostgreSQL, caches recent data in Redis, and broadcasts real-time updates via WebSocket/STOMP to connected clients (mobile/web apps built with Kotlin Multiplatform).

## Development Principles & Guidelines

**CRITICAL**: Follow these principles when working on this codebase. These are non-negotiable requirements for maintaining code quality and consistency.

### 1. Documentation-First Approach

**Always consult official documentation before implementing:**
- **Spring Boot**: https://spring.io/projects/spring-boot (Current version: 3.5.7)
- **Spring Framework**: https://docs.spring.io/spring-framework/reference/
- **Spring Integration**: https://docs.spring.io/spring-integration/reference/
- **Spring Data JPA**: https://docs.spring.io/spring-data/jpa/reference/
- **Spring Data Redis**: https://docs.spring.io/spring-data/redis/reference/
- **Kotlin**: https://kotlinlang.org/docs/home.html

**Use WebSearch/WebFetch tools to verify:**
- Current best practices (we are in 2025 - use current year in searches)
- Official recommendations for the specific Spring Boot version
- Migration guides if upgrading dependencies
- Security advisories and CVEs

**NEVER:**
- Make up API methods or classes that don't exist
- Assume behavior without verification
- Copy outdated Stack Overflow answers without checking official docs

### 2. Methodology & Tools

**ALWAYS utilize all available tools:**
- **WebSearch**: For current documentation, best practices, and official guides (remember: 2025)
- **WebFetch**: To read specific documentation pages
- **Task agents**: For complex multi-step operations or research
- **Grep/Glob**: To understand existing patterns in the codebase
- **Read**: To verify current implementation before making changes

**Before writing code:**
1. Search the codebase for similar existing implementations
2. Verify the pattern follows Spring Boot recommendations
3. Check if Spring Boot provides a built-in solution
4. Consult official documentation for the specific feature

**Never:**
- Hallucinate classes, methods, or configurations
- Invent solutions without verification
- Skip research for "obvious" implementations

### 3. Spring Boot Architecture Standards

**Follow Spring Boot recommended architectures:**
- **Layered Architecture**: Controller → Service → Repository
- **Dependency Injection**: Use constructor injection (immutable, testable)
- **Configuration**: Use `@ConfigurationProperties` for grouped settings
- **Events**: Use Spring ApplicationEvents for decoupling (already implemented)
- **Transactions**: Apply `@Transactional` at service layer with proper propagation
- **Validation**: Use Jakarta Bean Validation (`@Valid`, `@Validated`)

**Leverage Spring Boot features:**
- Auto-configuration where appropriate
- Actuator endpoints for monitoring
- Profiles for environment-specific configuration (`@Profile`)
- Conditional beans (`@ConditionalOnProperty`, etc.)
- Spring Boot Starters for dependency management

### 4. Code Quality Standards (SOLID Principles)

**Every piece of code must be:**

**S - Single Responsibility**
- Each class has ONE clear purpose
- Methods do ONE thing well
- Extract complex logic into separate, well-named methods

**O - Open/Closed**
- Use interfaces and abstractions for extensibility
- Prefer composition over inheritance
- Design for extension without modification

**L - Liskov Substitution**
- Subtypes must be substitutable for their base types
- Respect contracts defined by interfaces
- Don't weaken preconditions or strengthen postconditions

**I - Interface Segregation**
- Create focused, client-specific interfaces
- Don't force clients to depend on methods they don't use

**D - Dependency Inversion**
- Depend on abstractions, not concretions
- Use Spring's DI container effectively
- Constructor injection for required dependencies

**Readability Requirements:**
```kotlin
// ✅ GOOD: Clear, self-documenting, human-readable
class GreenhouseDataService(
    private val cacheService: GreenhouseCacheService,
    private val repository: SensorReadingRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    fun processAndCacheSensorReading(data: RealDataDto): SensorReading {
        val cachedData = cacheService.cacheMessage(data)
        val savedReading = repository.save(data.toSensorReading())
        eventPublisher.publishEvent(GreenhouseMessageEvent(this, data))
        return savedReading
    }
}

// ❌ BAD: Cryptic, hard to debug, machine-like
class GDS(private val cs: GCS, private val r: SRR, private val ep: AEP) {
    fun p(d: RDD) = r.save(d.toSR()).also { cs.c(d); ep.pub(GME(this, d)) }
}
```

**Debugging & Maintainability:**
- Descriptive variable names that explain WHAT, not just type
- Methods that fit on one screen (< 50 lines)
- Clear error messages with context
- Logging at appropriate levels (TRACE, DEBUG, INFO, WARN, ERROR)
- Comments for WHY, not WHAT (code should be self-documenting)

### 5. Scalability & Performance

**Design for production from day one:**
- **Database**: Use batch operations (`saveAll()` instead of loops with `save()`)
- **Caching**: Implement cache-aside pattern with proper TTL
- **Async Processing**: Use `@Async` for non-blocking operations
- **Connection Pooling**: Configure HikariCP properly (already configured)
- **Resource Management**: Use `use { }` for auto-closeable resources

**Monitor performance:**
- Add `@Timed` metrics for critical operations
- Use Spring Boot Actuator metrics
- Log slow queries (already configured in TimescaleDB)

### 6. Communication & Clarity

**When requirements are unclear:**

**ALWAYS use AskUserQuestion tool:**
- Don't assume or interpret ambiguous requirements
- Present clear options with trade-offs
- Validate understanding before implementing
- Ask about architectural decisions (which library, which pattern, etc.)

**Examples of when to ask:**
- "Should this data be cached or only stored in DB?"
- "Which error handling strategy: fail fast or retry with exponential backoff?"
- "Do you want synchronous or asynchronous processing?"
- "Should this be a REST endpoint or just internal service?"

**NEVER:**
- Implement based on guesses
- Choose arbitrarily between multiple valid approaches
- Leave important decisions to "interpretation"

### 7. Continuous Improvement

**Always propose improvements when you notice:**
- Violation of SOLID principles
- Code duplication (DRY violations)
- Missing error handling
- Potential performance bottlenecks
- Security vulnerabilities (SQL injection, XSS, etc.)
- Missing tests for critical logic
- Outdated dependencies with known issues
- Better Spring Boot alternatives to custom code

**Proactive suggestions:**
- "I notice X pattern. Would you like me to refactor using Y Spring Boot feature?"
- "This could be simplified using Spring's Z annotation"
- "Consider adding validation here to prevent invalid state"

### 8. Time Management & Task Planning

**Take the time needed to do it right:**
- Use **TodoWrite** for multi-step tasks to track progress
- Break complex features into manageable subtasks
- Create **custom Task agents** for complex research or implementation
- Don't rush - quality over speed

**For complex tasks:**
1. **Plan**: Create todo list with clear steps
2. **Research**: Verify approach with documentation
3. **Ask**: Clarify any ambiguities with AskUserQuestion
4. **Implement**: Write clean, tested code
5. **Review**: Check against SOLID principles
6. **Document**: Update relevant documentation

**Examples of when to use Task agents:**
- Exploring large codebases to understand patterns
- Researching best practices for new features
- Implementing complex multi-file refactorings
- Analyzing security implications

### 9. Context Awareness

**Remember the current date/time:**
- We are in **2025** (year 2025)
- Use current year in web searches for latest documentation
- Check for newer Spring Boot versions and migration guides
- Be aware of deprecated APIs in recent versions

**Version-specific considerations:**
- This project uses Spring Boot 3.5.7 (Spring 6.x)
- Java 21 features are available (virtual threads, pattern matching, etc.)
- Kotlin 2.2.21 with latest language features

---

## Build & Development Commands

### Build & Run
```bash
./gradlew build              # Build the project
./gradlew test               # Run all tests
./gradlew clean build -x test  # Build without tests (used in Docker)
./gradlew bootRun            # Run locally (requires services)
./gradlew dependencies       # View dependency tree
```

### Docker Compose (Local Development)
```bash
docker-compose up -d         # Start all services (API, TimescaleDB, PostgreSQL, Redis, EMQX)
docker-compose logs -f api   # View API logs
docker-compose ps            # Check service status
docker-compose down          # Stop all services
```

### Testing
```bash
./gradlew test --tests "ClassName"           # Run specific test class
./gradlew test --tests "ClassName.testName"  # Run specific test method
./gradlew test --info                        # Run tests with detailed output
```

## High-Level Architecture

### Core Pattern: Event-Driven IoT Data Pipeline

```
IoT Sensors (Greenhouse)
    ↓ MQTT WSS
EMQX Broker (Topic: "GREENHOUSE")
    ↓ Spring Integration MQTT Adapter
GreenhouseDataListener
    ↓
MqttMessageProcessor (@Transactional)
    ├→ Redis Cache (Sorted Set, last 1000 messages, 24h TTL)
    ├→ TimescaleDB (permanent time-series storage)
    └→ Spring ApplicationEvent (GreenhouseMessageEvent)
        ↓
GreenhouseWebSocketHandler (@EventListener)
    ↓ SimpMessagingTemplate
WebSocket/STOMP Clients (topic: /topic/greenhouse/messages)
```

### Dual Database Strategy

**TimescaleDB (Primary - Port 30432):**
- Time-series sensor readings (`SensorReading` entity)
- Optimized for time-based queries and aggregations
- Automatic compression and retention policies

**PostgreSQL (Metadata - Port 30433):**
- Reference data: `Greenhouse`, `User`, `Sensor`, `Actuator` entities
- Relationships and configuration data

### Key Architectural Patterns

1. **Cache-Aside Pattern**: Redis stores last 1000 messages as Sorted Set (score = timestamp)
2. **Event-Driven Architecture**: Spring ApplicationEvents decouple MQTT processing from WebSocket broadcasting
3. **Message-Driven Beans**: Spring Integration handles MQTT message routing
4. **Repository Pattern**: Separate repositories for timeseries vs metadata databases
5. **Dual DataSource Configuration**: Distinct connection pools and transaction managers

## Package Structure & Responsibilities

```
com.apptolast.invernaderos/
├── config/
│   ├── MqttConfig.kt              - Spring Integration MQTT (inbound/outbound adapters)
│   ├── WebSocketConfig.kt         - STOMP over WebSocket configuration
│   ├── TimescaleDataSourceConfig.kt   - Primary datasource (time-series)
│   └── PostGreSQLDataSourceConfig.kt  - Secondary datasource (metadata)
│
├── mqtt/
│   ├── listener/
│   │   ├── GreenhouseDataListener.kt  - Handles "GREENHOUSE" topic
│   │   ├── SensorDataListener.kt      - Handles sensor-specific topics
│   │   └── ActuatorStatusListener.kt  - Handles actuator status updates
│   ├── publisher/
│   │   └── MqttPublishService.kt      - Publishes to GREENHOUSE/RESPONSE
│   └── service/
│       └── MqttMessageProcessor.kt    - Core processing logic (Redis + DB + Events)
│
├── websocket/
│   └── GreenhouseWebSocketHandler.kt  - Broadcasts to WebSocket clients
│
├── service/
│   ├── GreenhouseDataService.kt       - Business logic (orchestrates cache + DB)
│   └── GreenhouseCacheService.kt      - Redis operations (Sorted Set)
│
├── entities/
│   ├── dtos/
│   │   ├── RealDataDto.kt             - CURRENT FORMAT (22 fields: temp/humidity/sectors/extractors)
│   │   ├── GreenhouseMessageDto.kt    - Legacy format (SENSOR_XX, SETPOINT_XX)
│   │   └── GreenhouseExtensions.kt    - JSON parsing extensions (toRealDataDto(), etc.)
│   ├── timescaledb/
│   │   └── SensorReading.kt           - Time-series entity
│   └── metadata/
│       ├── Greenhouse.kt, User.kt, Sensor.kt, Actuator.kt
│
├── repositories/
│   ├── timeseries/
│   │   └── SensorReadingRepository.kt  - TimescaleDB queries
│   └── metadata/
│       └── GreenhouseRepository.kt, etc.
│
└── controllers/
    └── GreenhouseController.kt         - REST endpoints
```

## Critical Configuration Details

### MQTT Integration (Spring Integration + Eclipse Paho)

**Inbound Adapter** (MqttConfig.kt:131-159):
- Subscribes to topics: `GREENHOUSE`, sensors, actuators, system events
- Client ID: `{prefix}-inbound-{UUID}`
- Clean session: `false` (persists session state)
- Automatic reconnect: `true`
- QoS: 0 (at most once)

**Outbound Adapter** (MqttConfig.kt:216-234):
- Publishes responses to `GREENHOUSE/RESPONSE`
- Client ID: `{prefix}-outbound-{UUID}`

**Message Routing** (MqttConfig.kt:166-203):
```kotlin
@ServiceActivator(inputChannel = "mqttInputChannel")
fun mqttMessageHandler(): MessageHandler {
    return MessageHandler { message ->
        val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String
        when {
            topic == "GREENHOUSE" -> greenhouseDataListener.handleGreenhouseData(message)
            // ... other topics
        }
    }
}
```

### WebSocket/STOMP Configuration

**Endpoints** (WebSocketConfig.kt):
- `ws://host/ws/greenhouse` - with SockJS fallback
- `ws://host/ws/greenhouse-native` - native WebSocket only

**STOMP Topics**:
- `/topic/greenhouse/messages` - Real-time sensor data (RealDataDto)
- `/topic/greenhouse/statistics` - Aggregated statistics

**Message Broker**:
- Simple in-memory broker
- Application destination prefix: `/app`
- User destination prefix: `/user`

### Redis Caching Strategy

**Implementation** (GreenhouseCacheService.kt):
```kotlin
Key: "greenhouse:messages"
Data Structure: Sorted Set
Score: timestamp.toEpochMilli()
Max Size: 1000 messages (ZREMRANGEBYRANK 0 -1001)
TTL: 24 hours
```

**Operations**:
- `cacheMessage(RealDataDto)` - Add to sorted set, trim to 1000
- `getRecentMessages(limit)` - ZREVRANGE (newest first)
- `getMessagesByTimeRange(start, end)` - ZRANGEBYSCORE

### Application Configuration Pattern

**File**: `src/main/resources/application.yaml`

**Key Environment Variables**:
```yaml
MQTT_BROKER_URL         # WSS URL for EMQX broker
MQTT_USERNAME           # MQTT credentials
MQTT_PASSWORD
TIMESCALE_PASSWORD      # TimescaleDB password
METADATA_PASSWORD       # PostgreSQL password
REDIS_HOST              # Redis connection
REDIS_PORT
REDIS_PASSWORD
```

## Data Models & Transformations

### RealDataDto (CURRENT FORMAT - 22 fields)

**File**: `entities/dtos/RealDataDto.kt`

**Structure**:
```kotlin
data class RealDataDto(
    val timestamp: Instant,
    @JsonProperty("TEMPERATURA INVERNADERO 01") val temperaturaInvernadero01: Double?,
    @JsonProperty("HUMEDAD INVERNADERO 01") val humedadInvernadero01: Double?,
    // ... 3 greenhouses (temperature + humidity)
    // ... 12 sector fields (INVERNADERO_XX_SECTOR_XX)
    // ... 3 extractor fields (INVERNADERO_XX_EXTRACTOR)
    val RESERVA: Double?,
    val greenhouseId: String? = null
)
```

**This is the DTO sent to mobile/web clients via WebSocket.**

### GreenhouseMessageDto (LEGACY FORMAT)

**File**: `entities/dtos/GreenhouseMessageDto.kt`

**Structure**: Contains `sensor01`, `sensor02`, `setpoint01-03`, `rawPayload`

**Note**: Has unused `randomDatafromGreenHouseTopic()` method (lines 64-70)

### JSON Parsing Extensions

**File**: `entities/dtos/GreenhouseExtensions.kt`

**Key Functions**:
- `String.toRealDataDto(timestamp, greenhouseId)` - Parse JSON to RealDataDto (lines 86-121)
- `String.toGreenhouseMessageDto(timestamp, greenhouseId)` - Parse JSON to legacy format
- `RealDataDto.toJson()` - Serialize to JSON with @JsonProperty mapping

## Important Flows

### MQTT Message Processing (MqttMessageProcessor.kt:98-155)

```kotlin
fun processGreenhouseData(payload: String, greenhouseId: String) {
    // 1. Parse JSON to RealDataDto
    val messageDto = payload.toRealDataDto(Instant.now(), greenhouseId)

    // 2. Cache in Redis (sorted set)
    cacheService.cacheMessage(messageDto)

    // 3. Save to TimescaleDB (batch insert optimization)
    val readings = messageDto.toSensorReadings()
    repository.saveAll(readings)  // Batch insert

    // 4. Publish Spring event (async)
    publisher.publishEvent(GreenhouseMessageEvent(this, messageDto))
}
```

### WebSocket Broadcasting (GreenhouseWebSocketHandler.kt:39-55)

```kotlin
@EventListener
fun handleGreenhouseMessage(event: GreenhouseMessageEvent) {
    messagingTemplate.convertAndSend(
        "/topic/greenhouse/messages",
        event.message  // RealDataDto sent to clients
    )
}
```

## Technology Stack

- **Runtime**: Java 21, Kotlin 2.2.21
- **Framework**: Spring Boot 3.5.7
- **Build Tool**: Gradle 8.14.3 with Kotlin DSL
- **MQTT**: Spring Integration MQTT 6.5.3 + Eclipse Paho 1.2.5
- **Databases**: TimescaleDB (time-series), PostgreSQL 16 (metadata)
- **Cache**: Redis 7 with Lettuce client
- **WebSocket**: Spring WebSocket + STOMP
- **Serialization**: Jackson with @JsonProperty support
- **API Docs**: SpringDoc OpenAPI 2.8.14 (Swagger UI)
- **Containerization**: Docker with multi-stage builds
- **Monitoring**: Spring Boot Actuator

## CI/CD

**GitHub Actions**: `.github/workflows/build-and-push.yml`
- Builds on push to `main` or `develop` branches
- Pushes Docker images to DockerHub:
  - `apptolast/invernaderos-api:latest` (main branch)
  - `apptolast/invernaderos-api:develop` (develop branch)

## Additional Documentation

- **README.md** - Comprehensive bilingual documentation (EN/ES) with analogies
- **GREENHOUSE_MQTT_IMPLEMENTATION.md** - Detailed MQTT implementation guide
- **DEPLOYMENT.md** - Docker/Kubernetes deployment instructions
- **SECURITY.md** - Security guidelines and best practices
- **SECURITY_AUDIT_REPORT.md** - Security audit findings

## Common Gotchas

1. **Dual DataSource**: Always specify `@Qualifier` when injecting repositories or transaction managers
2. **MQTT Topics**: Topic `GREENHOUSE` is for inbound data, `GREENHOUSE/RESPONSE` is for echoing back to broker
3. **WebSocket vs MQTT**: Mobile apps receive data via WebSocket (STOMP), not directly from MQTT
4. **DTO Format**: System currently uses `RealDataDto` (22 fields), not `GreenhouseMessageDto`
5. **Redis Score**: Sorted set uses `timestamp.toEpochMilli()` as score for time-based queries
6. **Batch Inserts**: Use `repository.saveAll()` for TimescaleDB to optimize bulk inserts
7. **JSON Mapping**: RealDataDto uses `@JsonProperty` with spaces ("TEMPERATURA INVERNADERO 01")

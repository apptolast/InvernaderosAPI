# InvernaderosAPI — IoT Greenhouse Management System

## Stack
- **Spring Boot** 3.5.7 + **Kotlin** 2.2.21 (Java 21 LTS)
- **PostgreSQL** 16 (metadata schema) + **TimescaleDB** (iot schema, time-series)
- **MQTT** via Spring Integration 6.5.3 + Eclipse Paho 1.2.5
- **Redis** 7 (Lettuce client, sorted set cache)
- **WebSocket/STOMP** for real-time sensor data
- **Flyway** for migrations, **ArchUnit** for architecture tests

## Build & Test
```bash
./gradlew build                    # Full build
./gradlew compileKotlin            # Compile only
./gradlew test                     # All tests
./gradlew test --tests "*ArchitectureTest*"  # Architecture tests only
./gradlew test --tests "*.features.greenhouse.*"  # Module tests
```

## Architecture: Hexagonal (Ports & Adapters)

Each feature module follows this structure:
```
features/{module}/
├── domain/                  # Pure Kotlin, ZERO Spring/JPA/Jakarta imports
│   ├── model/               # Data classes, @JvmInline value objects
│   ├── port/
│   │   ├── input/           # Use case interfaces (driving ports)
│   │   └── output/          # Repository interfaces (driven ports)
│   └── error/               # Sealed interface {Module}Error
├── application/
│   └── usecase/             # Use case implementations (orchestration)
├── infrastructure/
│   ├── adapter/
│   │   ├── input/           # REST controllers, MQTT listeners
│   │   └── output/          # JPA entities + repos + adapter (implements domain port)
│   └── config/              # @Configuration for bean wiring
└── dto/
    ├── request/             # One file per request DTO
    ├── response/            # One file per response DTO
    └── mapper/              # Extension functions: .toDomain(), .toResponse(), .toEntity()
```

### Rules (NON-NEGOTIABLE)
1. **domain/ is pure Kotlin**: No `org.springframework`, `jakarta.persistence`, `jakarta.validation`
2. **Sealed interfaces for errors**: `sealed interface GreenhouseError { val message: String }`
3. **@JvmInline value classes for IDs**: `@JvmInline value class GreenhouseId(val value: Long)`
4. **Extension functions for mappers**: No Mapper classes, use `.toDomain()`, `.toResponse()`, `.toEntity()`
5. **One class per file**: No 640-line DTOs files. One DTO = one file.
6. **Constructor injection ALWAYS**: No `@Autowired` fields, no `lateinit var`
7. **nativeQuery = true**: Mandatory for TimescaleDB functions (time_bucket, etc)
8. **Strangler Fig**: New code wraps old, verify parity with tests, then remove old

## Agent Teams — File Ownership (CRITICAL)

Two agents must NEVER edit the same file simultaneously.

| Agent | Owns | Must NOT touch |
|-------|------|----------------|
| **domain-architect** | `docs/architecture/**` | `src/main/**` (read-only) |
| **domain-implementer** | `features/{module}/domain/**` | `infrastructure/`, `dto/` |
| **adapter-implementer** | `features/{module}/infrastructure/**`, `dto/**`, `application/**` | `domain/` |
| **test-writer** | `src/test/**` | `src/main/**` (read-only) |
| **code-reviewer** | None (read-only) | All files (read-only) |

## Coding Conventions

### Naming
- Classes: `PascalCase` descriptive (not `GDS`, `SRR`, or `Util`)
- Methods: verb + noun (`createGreenhouse`, `findByTenantId`)
- Variables: descriptive (`sensorReading`, not `sr` or `data`)
- Test methods: backtick style `` `should create greenhouse when name is unique` ``

### Error Handling
- Domain errors: `sealed interface` with context in message
- HTTP errors: Map `Either.Left` to proper status codes (404, 409, 422)
- Logging: DEBUG for flow, INFO for results, WARN for anomalies, ERROR + exception for failures
- Error messages: Include entity type, ID, and what went wrong

### Database
- Dual datasource: always use `@Qualifier` for transaction managers
- TimescaleDB schema: `iot` (NOT `public`)
- PostgreSQL schema: `metadata`
- Schema changes: Flyway migration ALWAYS (never manual DDL)

## Documentation (Context7 MCP)
- ALWAYS use Context7 MCP for library documentation lookups
- Spring Boot: `/spring-projects/spring-boot`
- TimescaleDB: `/timescale/timescaledb`
- Spring Data JPA: `/spring-projects/spring-data-jpa`
- For Kotlin: resolve library ID first with `resolve-library-id`
- Do NOT rely on training data for API references — always fetch via Context7

## Task Sizing
- Each task completable in 5-15 minutes
- Include clear acceptance criteria in each task
- Target: 5-6 tasks per teammate
- Each task = one atomic descriptive commit

## Quality Gates
- ALL code must pass: `./gradlew compileKotlin && ./gradlew test`
- ArchUnit tests must pass before any module is considered done
- Code reviewer signs off before closing

## Communication
- Use SendMessage for coordinating interfaces/contracts between layers
- Team Lead synthesizes and resolves conflicts
- Direct messages for specific needs (not broadcast)
- Broadcasts only for announcements affecting the whole team

## Legacy Reference
Full project documentation (2000+ lines, may be outdated): see `CLAUDE_LEGACY.md`

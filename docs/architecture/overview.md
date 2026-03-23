# InvernaderosAPI — Architecture Overview

## System Overview

IoT Greenhouse Management System built with Spring Boot 3.5.7 + Kotlin 2.2.21.
Multi-tenant, dual-database (PostgreSQL metadata + TimescaleDB time-series), MQTT-connected.

## Architecture: Hexagonal (Ports & Adapters)

```
                    ┌─────────────────────────────────────┐
                    │            REST / MQTT               │
                    │         (Input Adapters)              │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │          Use Cases                    │
                    │       (Application Layer)             │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │        Domain Model                   │
                    │   (Pure Kotlin, ZERO framework)       │
                    │   Models, Ports, Sealed Errors         │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │     JPA / TimescaleDB / MQTT          │
                    │        (Output Adapters)               │
                    └─────────────────────────────────────┘
```

## Module Structure (per feature)

```
features/{module}/
├── domain/                  # Pure Kotlin, ZERO Spring/JPA imports
│   ├── model/               # Data classes, @JvmInline value objects
│   ├── port/
│   │   ├── input/           # Use case interfaces (driving ports)
│   │   └── output/          # Repository interfaces (driven ports)
│   └── error/               # Sealed interface {Module}Error
├── application/
│   └── usecase/             # Use case implementations
├── infrastructure/
│   ├── adapter/
│   │   ├── input/           # REST controllers
│   │   └── output/          # JPA adapters, code generators
│   └── config/              # @Configuration bean wiring
└── dto/
    ├── request/             # One file per request DTO
    ├── response/            # One file per response DTO
    └── mapper/              # Extension functions
```

## Shared Domain Types

Located in `features/shared/domain/`:

| Type | File | Purpose |
|------|------|---------|
| `Either<L,R>` | `Either.kt` | Error handling (Left=error, Right=success) |
| `TenantId` | `model/TenantId.kt` | @JvmInline value class |
| `GreenhouseId` | `model/GreenhouseId.kt` | @JvmInline value class |
| `SectorId` | `model/SectorId.kt` | @JvmInline value class |
| `DeviceId` | `model/DeviceId.kt` | @JvmInline value class |
| `SettingId` | `model/SettingId.kt` | @JvmInline value class |
| `Location` | `model/Location.kt` | Geographic coordinates |

## Module Dependency Map

```
Greenhouse ← Sector ← Device ← CommandHistory
     ↑           ↑        ↑
   Tenant    Alert    Setting
     ↑
   User ← Auth
```

Cross-module ports:
- `GreenhouseExistencePort` (in Sector) — validates greenhouse belongs to tenant
- `SectorExistencePort` (in Device) — validates sector belongs to tenant
- `SectorValidationPort` (in Alert, Setting) — same pattern
- `CodeExistencePort` (in Command) — validates device/setting codes exist
- `CommandPublisherPort` (in Command) — abstracts MQTT publishing
- `PasswordHasher` (in User) — abstracts Spring Security PasswordEncoder

## Infrastructure (Not Refactored)

These components remain as-is (well-isolated infrastructure):
- **MQTT Pipeline**: 18 files in `mqtt/` (listeners, processors, publisher, dedup, rate limiter)
- **TimescaleDB entities/repos**: `features/telemetry/` (SensorReading, DeviceCommand, etc.)
- **Spring Security**: `core/security/` (JWT, filters, config)
- **WebSocket**: `features/websocket/` (CQRS read assembler)
- **Statistics**: `features/statistics/` (read-only aggregation)

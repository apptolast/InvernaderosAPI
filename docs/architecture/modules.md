# Module Refactoring Status

## Full Hexagonal (8 modules)

### 1. Greenhouse
- **Domain**: Greenhouse, GreenhouseId, GreenhouseError (NotFound, DuplicateName)
- **Use Cases**: Create, Find, Update, Delete
- **Ports**: GreenhouseRepositoryPort, GreenhouseCodeGenerator
- **Endpoints**: `/api/v1/tenants/{tenantId}/greenhouses` (CRUD)
- **Tests**: 4 test files (12 tests)

### 2. Tenant
- **Domain**: Tenant, TenantStatus (ACTIVE/INACTIVE/PENDING), TenantError (NotFound, DuplicateName, DuplicateEmail)
- **Use Cases**: Create, Find (with TenantFilter), Update, Delete
- **Key Decision**: TenantStatus enum replaces Boolean? tri-state. Name/email uniqueness validation added (was 500, now 409).
- **Endpoints**: `/api/v1/tenants` (CRUD + filters)
- **Tests**: 4 test files (14 tests)

### 3. Sector
- **Domain**: Sector, SectorError (NotFound, GreenhouseNotFound, GreenhouseNotOwnedByTenant)
- **Cross-module Port**: GreenhouseExistencePort — validates greenhouse belongs to tenant
- **Endpoints**: `/api/v1/tenants/{tenantId}/sectors` (CRUD) + `/api/v1/greenhouses/{id}/sectors` (GET)
- **Tests**: 4 test files (11 tests)

### 4. Device + CommandHistory
- **Domain**: Device (enriched with sectorCode, categoryName, typeName, unitSymbol), CommandHistory, DeviceError
- **Cross-module Port**: SectorExistencePort
- **Endpoints**: `/api/v1/tenants/{tenantId}/devices` (CRUD + command history)
- **Tests**: 5 test files (13 tests)

### 5. Sensor API + Command API
- **Sensor**: QuerySensorReadingsUseCase (read-only, wraps TimescaleDB SensorReadingRepository)
- **Command**: SendCommandUseCase + QueryCommandHistoryUseCase. Ports: DeviceCommandPersistencePort, CodeExistencePort, CommandPublisherPort (wraps MqttPublisher)
- **Endpoints**: `/api/v1/sensors` (3 GET) + `/api/v1/commands` (POST + GET)
- **Tests**: 3 test files (10 tests)

### 6. Alert
- **Domain**: Alert (with resolve/reopen), AlertError (NotFound, SectorNotOwnedByTenant, AlreadyResolved)
- **Use Cases**: Create, Find, Update, Delete, **Resolve** (resolve + reopen)
- **Key Decision**: AlertService kept alive — AlertController legacy (`/api/v1/alerts`) uses it directly
- **Endpoints**: `/api/v1/tenants/{tenantId}/alerts` (CRUD + resolve + reopen)
- **Tests**: 5 test files (15 tests)

### 7. Setting
- **Domain**: Setting (enriched with parameterName, actuatorStateName, dataTypeName), SettingError
- **Cross-module Port**: SettingSectorValidationPort
- **Endpoints**: `/api/v1/tenants/{tenantId}/settings` (CRUD + sector/parameter/actuatorState filters)
- **Tests**: 4 test files (8 tests)

### 8. User
- **Domain**: User (NO passwordHash in domain model), UserRole enum, UserError (NotFound, DuplicateUsername, DuplicateEmail, InvalidRole)
- **Port**: PasswordHasher — abstracts Spring Security PasswordEncoder
- **Key Decision**: UserService kept alive — AuthService uses it for login/register/password-reset
- **Endpoints**: `/api/v1/tenants/{tenantId}/users` (CRUD)
- **Tests**: 4 test files

## Pragmatic (3 modules)

### 9. Catalog
- **Approach**: Split only (no hexagonal domain layer)
- `CatalogDTOs.kt` (641 lines) → 25 individual files (8 response + 16 request + 1 mapper)
- `CatalogController.kt` (843 lines) → 7 individual controllers
- Services and entities unchanged

### 10. Auth
- **Approach**: Split DTOs only
- `AuthDTOs.kt` → 5 files (4 request + 1 response)
- AuthService, AuthController, EmailService unchanged (Spring Security infrastructure)

### 11. Statistics + WebSocket
- **Approach**: No changes needed
- Statistics: already moved from greenhouse in Phase 0
- WebSocket: CQRS read assembler with 8 repositories — well-isolated

## Strangler Fig: Legacy Files Preserved

These JPA entities and Spring Data repositories are kept at their original locations because other modules (WebSocket, MQTT, Catalog) still reference them directly:

- All `{Module}.kt` entity files (Greenhouse, Tenant, Sector, Device, Alert, Setting, User)
- All `{Module}Repository.kt` Spring Data interfaces
- `AlertService.kt` (used by legacy AlertController)
- `UserService.kt` (used by AuthService)
- `LocationDto.kt` (used by greenhouse DTOs and WebSocket)

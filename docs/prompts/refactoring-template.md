# Template Reutilizable: Refactoring de un modulo

Reemplaza `{MODULE}` y `{module}` con el nombre del modulo.
Reemplaza `{ARCHIVOS_ACTUALES}` con la lista de archivos del modulo actual.

---

```
Refactorizar el modulo de {MODULE} de InvernaderosAPI usando el mismo patron
hexagonal establecido en el modulo greenhouse.

Los endpoints REST existentes DEBEN seguir funcionando identicamente.
Estrategia: Strangler Fig (envolver, verificar, eliminar).

Crea un agent team con estos teammates:

**Teammate 1 — "domain-architect"** (agente .claude/agents/domain-architect.md):
"PREAMBLE: WORKER, NO spawnes otros agentes.
Analiza todo el codigo actual en features/{module}/:
{ARCHIVOS_ACTUALES}
Mapea dependencias, disena estructura hexagonal destino.
Crea docs/architecture/{module}.md con plan archivo-por-archivo.
USA Context7 MCP."

**Teammate 2 — "domain-implementer"** (agente .claude/agents/domain-implementer.md):
"PREAMBLE: WORKER, NO spawnes otros agentes.
Tras completar domain-architect:
Implementa domain models, value objects, sealed errors, port interfaces.
CERO Spring annotations.
USA Context7. Ownership: features/{module}/domain/**"

**Teammate 3 — "adapter-implementer"** (agente .claude/agents/adapter-implementer.md):
"PREAMBLE: WORKER, NO spawnes otros agentes.
Tras completar domain-implementer:
Implementa JPA entities, Spring Data repos, adapter classes, controllers,
use case implementations, DTOs (un archivo por DTO), extension mappers,
@Configuration para wiring.
Preserva mismos endpoints REST.
USA Context7. Ownership: features/{module}/infrastructure/**, dto/**, application/**"

**Teammate 4 — "test-writer"** (agente .claude/agents/test-writer.md):
"PREAMBLE: WORKER, NO spawnes otros agentes.
Tras completar adapter-implementer:
Unit tests dominio (MockK, sin Spring), integration tests, ArchUnit verify,
regression tests para endpoints existentes.
USA Context7. Ownership: src/test/**"

**Teammate 5 — "code-reviewer"** (agente .claude/agents/code-reviewer.md):
"PREAMBLE: WORKER de SOLO LECTURA.
Tras completar test-writer:
Revisa adherencia a hexagonal. Reporta findings priorizados."

Dependencias: 1→2→3→4→5
Plan approval requerido.
./gradlew compileKotlin tras implementacion.
./gradlew test tras tests.
Delegate Mode + tmux.
```

---

## Orden de modulos recomendado

| # | Modulo | Archivos actuales clave |
|---|--------|------------------------|
| 1 | greenhouse | GreenHouse.kt, GreenhouseService.kt, GreenhouseController.kt, TenantGreenhouseController.kt, GreenhouseRepository.kt, GreenhouseDTOs.kt |
| 2 | tenant | Tenant.kt, TenantService.kt, TenantController.kt, TenantRepository.kt, TenantDTOs.kt |
| 3 | sector | Sector.kt, SectorService.kt, SectorRepository.kt, GreenhouseSectorController.kt, TenantSectorController.kt, SectorDTOs.kt |
| 4 | device + command | Device.kt, DeviceService.kt, DeviceRepository.kt, TenantDeviceController.kt, DeviceDTOs.kt, CommandHistory.kt, DeviceCommandController.kt, DeviceCommandService.kt |
| 5 | telemetry + sensor | SensorReading.kt, Reading.kt, DeviceCurrentValue.kt, SensorReadingRaw.kt, DeviceCommand.kt, SensorReadingRepository.kt, ReadingRepository.kt, MqttMessageProcessor.kt, SensorDataListener.kt, SensorReadingController.kt, SensorReadingService.kt |
| 6 | catalog | ActuatorState.kt, AlertSeverity.kt, AlertType.kt, DataType.kt, DeviceCategory.kt, DeviceType.kt, Period.kt, Unit.kt (+ 7 services, 7 repos, CatalogController.kt, CatalogDTOs.kt 640 lineas) |
| 7 | alert | Alert.kt, AlertService.kt, AlertRepository.kt, AlertController.kt, TenantAlertController.kt, AlertDTOs.kt |
| 8 | setting | Setting.kt, SettingService.kt, SettingRepository.kt, TenantSettingController.kt, SettingDTOs.kt |
| 9 | user + auth | User.kt, UserService.kt, UserRepository.kt, TenantUserController.kt, UserDTOs.kt, AuthService.kt, AuthController.kt, AuthDTOs.kt, EmailService.kt |
| 10 | statistics + websocket | StatisticsController.kt, StatisticsService.kt, StatsDao.kt, GreenhouseStatisticsDto.kt, GreenhouseStatusWebSocketController.kt, GreenhouseStatusAssembler.kt |

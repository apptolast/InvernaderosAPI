# Prompt Maestro: Fase 0 (Infraestructura) + Fase 1 (Greenhouse)

Copia y pega este prompt en una nueva sesion de Claude Code con Agent Teams habilitado.

---

```
Necesito refactorizar InvernaderosAPI hacia arquitectura hexagonal. Vamos a hacer
dos cosas en esta sesion: primero preparar la infraestructura base, y luego
refactorizar el modulo GREENHOUSE como primer modulo piloto.

ESTRATEGIA: Strangler Fig — el codigo nuevo envuelve al viejo, verificamos
paridad con tests, y despues eliminamos lo antiguo. Los endpoints REST existentes
DEBEN seguir funcionando identicamente.

Crea un agent team con estos teammates:

**Teammate 1 — "domain-architect"** (agente .claude/agents/domain-architect.md):
"PREAMBLE: Eres un WORKER, NO spawnes otros agentes.

FASE 0 - Infraestructura base:
1. Analiza la estructura actual del proyecto completo con Grep/Glob
2. Crea el archivo features/shared/domain/Either.kt con un tipo Either<L,R> simple
   (sealed class con Left y Right, sin dependencias externas)
3. Documenta en docs/architecture/overview.md:
   - Mapa de bounded contexts identificados
   - Dependencias entre modulos
   - Orden de refactoring recomendado

FASE 1 - Greenhouse:
4. Analiza TODO el codigo en features/greenhouse/:
   - GreenHouse.kt (entidad JPA)
   - GreenhouseService.kt (logica + queries)
   - GreenhouseController.kt y TenantGreenhouseController.kt
   - GreenhouseRepository.kt
   - dto/GreenhouseDTOs.kt
5. Mapea dependencias: que usa, quien lo usa
6. Disena la estructura hexagonal destino para greenhouse
7. Define interfaces Kotlin de cada puerto (input + output)
8. Crea docs/architecture/greenhouse.md con el plan archivo-por-archivo

USA Context7 MCP con /spring-projects/spring-boot para patrones Spring.
Output: docs/architecture/overview.md + docs/architecture/greenhouse.md"

**Teammate 2 — "domain-implementer"** (agente .claude/agents/domain-implementer.md):
"PREAMBLE: Eres un WORKER, NO spawnes otros agentes.
Tras completar domain-architect:

Implementa la capa de dominio para el modulo greenhouse.

Crea en features/greenhouse/domain/:
- model/Greenhouse.kt — data class pura Kotlin (id, tenantId, name, code, location, areaM2, timezone, isActive)
- model/GreenhouseId.kt — @JvmInline value class GreenhouseId(val value: Long)
- model/TenantId.kt — @JvmInline value class TenantId(val value: Long)
  (o en features/shared/domain/model/TenantId.kt si es compartido)
- error/GreenhouseError.kt — sealed interface con variantes: NotFound, DuplicateName, InvalidData
- port/input/CreateGreenhouseUseCase.kt — interfaz
- port/input/FindGreenhouseUseCase.kt — interfaz
- port/input/UpdateGreenhouseUseCase.kt — interfaz
- port/input/DeleteGreenhouseUseCase.kt — interfaz
- port/output/GreenhouseRepository.kt — interfaz del repositorio (NO Spring Data)

CERO anotaciones Spring. CERO imports de org.springframework o jakarta.
USA Context7 para verificar patrones Kotlin idiomaticos.
Ownership: features/greenhouse/domain/**"

**Teammate 3 — "adapter-implementer"** (agente .claude/agents/adapter-implementer.md):
"PREAMBLE: Eres un WORKER, NO spawnes otros agentes.
Tras completar domain-implementer:

Implementa los adaptadores de infraestructura para greenhouse.

Crea:
- infrastructure/adapter/output/GreenhouseEntity.kt — entidad JPA (table greenhouses, schema metadata)
- infrastructure/adapter/output/GreenhouseJpaRepository.kt — Spring Data repository
- infrastructure/adapter/output/GreenhouseRepositoryAdapter.kt — implementa puerto de dominio
- infrastructure/adapter/input/GreenhouseController.kt — REST controller nuevo
- infrastructure/adapter/input/TenantGreenhouseController.kt — preserva mismos endpoints
- infrastructure/config/GreenhouseModuleConfig.kt — @Configuration para wiring
- application/usecase/CreateGreenhouseUseCaseImpl.kt
- application/usecase/FindGreenhouseUseCaseImpl.kt
- application/usecase/UpdateGreenhouseUseCaseImpl.kt
- application/usecase/DeleteGreenhouseUseCaseImpl.kt
- dto/request/GreenhouseCreateRequest.kt — un DTO por archivo
- dto/request/GreenhouseUpdateRequest.kt
- dto/response/GreenhouseResponse.kt
- dto/mapper/GreenhouseMappers.kt — extension functions

Los endpoints REST deben ser IDENTICOS a los actuales (mismas URLs, mismos campos JSON).
USA Context7 con /spring-projects/spring-boot para patrones JPA.
Ownership: features/greenhouse/infrastructure/**, dto/**, application/**"

**Teammate 4 — "test-writer"** (agente .claude/agents/test-writer.md):
"PREAMBLE: Eres un WORKER, NO spawnes otros agentes.
Tras completar adapter-implementer:

Escribe tests completos para el modulo greenhouse refactorizado.

Crea:
- architecture/HexagonalArchitectureTest.kt — ArchUnit: domain/ sin Spring, adapters implementan ports
- features/greenhouse/domain/usecase/CreateGreenhouseUseCaseTest.kt — unit test con MockK
- features/greenhouse/domain/usecase/FindGreenhouseUseCaseTest.kt
- features/greenhouse/domain/usecase/UpdateGreenhouseUseCaseTest.kt
- features/greenhouse/domain/usecase/DeleteGreenhouseUseCaseTest.kt
- features/greenhouse/infrastructure/GreenhouseControllerIntegrationTest.kt — regression test

Usa MockK (NO Mockito) para los tests de dominio.
Los tests de regresion deben verificar que los endpoints devuelven las mismas respuestas.
USA Context7 para patrones de testing con Spring Boot.
Ownership: src/test/**"

**Teammate 5 — "code-reviewer"** (agente .claude/agents/code-reviewer.md):
"PREAMBLE: Eres un WORKER de SOLO LECTURA, NO spawnes otros agentes.
Tras completar test-writer:

Revisa TODO el codigo generado para greenhouse contra el checklist:
- domain/ sin imports de Spring/JPA/Jakarta
- Sealed interfaces para errores
- @JvmInline para IDs
- Extension functions para mappers
- Un DTO por archivo
- Constructor injection (no @Autowired)
- Error messages con contexto
- Logging con niveles apropiados
- No secrets en codigo

Reporta findings al team-lead priorizados: rojo (bloquea) / amarillo (deberia arreglar) / verde (sugerencia)."

Dependencias de tareas:
- Task 1: Analisis y plan (domain-architect) → sin dependencias
- Task 2: Implementar dominio (domain-implementer) → bloqueada por Task 1
- Task 3: Implementar adaptadores (adapter-implementer) → bloqueada por Task 2
- Task 4: Tests completos (test-writer) → bloqueada por Task 3
- Task 5: Code review (code-reviewer) → bloqueada por Task 4

REGLAS:
- Requiere plan approval antes de que cualquier teammate toque codigo
- Ejecutar ./gradlew compileKotlin tras cada task de implementacion
- Ejecutar ./gradlew test tras Task 4
- Todos los agentes DEBEN usar Context7 MCP para documentacion
- Activa Delegate Mode inmediatamente (Shift+Tab)
- Usa tmux para split panes (ya esta instalado)
```

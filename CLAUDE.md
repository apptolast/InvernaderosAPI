# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**InvernaderosAPI** is a production IoT greenhouse monitoring system that receives sensor data via MQTT, stores it in TimescaleDB/PostgreSQL, caches recent data in Redis, and broadcasts real-time updates via WebSocket/STOMP to connected clients (mobile/web apps built with Kotlin Multiplatform).

---

## ⚠️ PRINCIPIOS FUNDAMENTALES (NON-NEGOTIABLE) ⚠️

**ANTES DE HACER CUALQUIER COSA, LEE ESTO:**

1. **🔍 DOCUMENTACIÓN OFICIAL PRIMERO - SIEMPRE**
   - ❌ NUNCA inventes clases, métodos o configuraciones
   - ✅ SIEMPRE consulta https://spring.io/projects/spring-boot ANTES de implementar
   - ✅ SIEMPRE usa WebSearch/WebFetch para verificar (recuerda: estamos en **2025**)

2. **🛠️ USA TODAS TUS HERRAMIENTAS - NO ADIVINES**
   - ✅ WebSearch para documentación actualizada (año 2025)
   - ✅ WebFetch para leer páginas específicas
   - ✅ Task agents para investigaciones complejas
   - ✅ Grep/Glob para entender patrones existentes
   - ❌ NUNCA asumas comportamiento sin verificar

3. **🏗️ ARQUITECTURA SPRING BOOT - SIGUE LAS RECOMENDACIONES OFICIALES**
   - ✅ Si Spring Boot tiene una solución nativa, ÚSALA
   - ✅ Sigue patrones recomendados oficialmente
   - ❌ No reinventes lo que Spring Boot ya proporciona

4. **📖 CÓDIGO HUMANO, NO CÓDIGO MÁQUINA**
   - ✅ Nombres descriptivos que explican QUÉ hacen
   - ✅ Métodos que caben en una pantalla (< 50 líneas)
   - ✅ SOLID principles en TODO el código
   - ❌ NO abreviar nombres por ahorrar caracteres
   - ❌ NO crear código críptico difícil de debuggear

5. **❓ CUANDO TENGAS DUDAS - PREGUNTA**
   - ✅ USA AskUserQuestion tool si algo no está claro
   - ❌ NUNCA asumas o interpretes requisitos ambiguos
   - ❌ NUNCA elijas arbitrariamente entre múltiples opciones válidas

6. **💡 PROPÓN MEJORAS PROACTIVAMENTE**
   - ✅ SIEMPRE sugiere mejoras cuando veas problemas
   - ✅ Identifica violaciones de SOLID, duplicación, problemas de performance
   - ✅ Propón alternativas de Spring Boot a código custom

7. **⏰ CALIDAD SOBRE VELOCIDAD - TÓMATE EL TIEMPO NECESARIO**
   - ✅ Usa TodoWrite para planificar tareas complejas
   - ✅ Crea Task agents personalizados cuando sea necesario
   - ❌ NO tengas prisa - hazlo bien la primera vez

8. **📅 CONTEXTO TEMPORAL: ESTAMOS EN 2025**
   - ✅ USA "2025" en búsquedas web
   - ✅ Busca documentación actualizada
   - ✅ Verifica APIs no deprecadas en versiones recientes

---

## Development Principles & Guidelines

**CRITICAL**: Follow these principles when working on this codebase. These are non-negotiable requirements for maintaining code quality and consistency.

### 1. Documentation-First Approach

**⚠️ REGLA DE ORO: DOCUMENTACIÓN OFICIAL ANTES QUE NADA ⚠️**

**ANTES DE ESCRIBIR UNA SOLA LÍNEA DE CÓDIGO, consulta la documentación oficial:**

- **Spring Boot**: https://spring.io/projects/spring-boot (Current version: 3.5.7)
  - ⚠️ **IMPORTANTE**: Estamos en **2025** - busca docs actualizadas de 2025
- **Spring Framework**: https://docs.spring.io/spring-framework/reference/
- **Spring Integration**: https://docs.spring.io/spring-integration/reference/
- **Spring Data JPA**: https://docs.spring.io/spring-data/jpa/reference/
- **Spring Data Redis**: https://docs.spring.io/spring-data/redis/reference/
- **Kotlin**: https://kotlinlang.org/docs/home.html

**OBLIGATORIO - Usa WebSearch/WebFetch para CADA feature nueva:**
1. **Búsqueda actualizada**: Incluye "2025" o "Spring Boot 3.5" en tus búsquedas
2. **Best practices oficiales**: ¿Qué recomienda Spring Boot para este caso?
3. **Migration guides**: ¿Hay cambios en versiones recientes?
4. **Security advisories**: ¿Hay CVEs o vulnerabilidades conocidas?
5. **Spring Boot nativo**: ¿Spring Boot ya tiene esta funcionalidad?

**Ejemplos de cuándo DEBES buscar documentación:**
- ✅ Antes de crear un @Configuration: "Spring Boot 3.5 configuration best practices 2025"
- ✅ Antes de implementar caching: "Spring Boot Redis cache configuration 2025"
- ✅ Antes de usar una anotación: "Spring @Transactional propagation levels 2025"
- ✅ Antes de configurar datasources: "Spring Boot multiple datasources configuration 2025"

**❌ ABSOLUTAMENTE PROHIBIDO:**
- ❌ Inventar clases que no existen (ej: `SpringBootHelper`, `DataSourceManager`)
- ❌ Inventar métodos de APIs (ej: `.autoSave()`, `.enableCaching()`)
- ❌ Asumir comportamiento sin verificar en docs oficiales
- ❌ Copiar código de Stack Overflow sin verificar que esté actualizado y sea correcto
- ❌ Usar patrones "porque funcionan" sin verificar que sean recommended by Spring Boot
- ❌ Implementar features manualmente cuando Spring Boot las proporciona nativamente

### 2. Methodology & Tools

**🛠️ USA TODAS LAS HERRAMIENTAS - ES OBLIGATORIO, NO OPCIONAL 🛠️**

Tienes herramientas poderosas a tu disposición. **ÚSALAS SIEMPRE**. No adivines, no asumas, no inventes.

**Herramientas disponibles y CUÁNDO usarlas:**

1. **WebSearch** - Para documentación y best practices actualizadas
   - ✅ Cuando necesites saber cómo hacer algo en Spring Boot
   - ✅ Cuando busques examples de configuración
   - ✅ Cuando quieras verificar si existe una feature
   - 🔴 **RECUERDA**: Incluye "**2025**" en tus búsquedas
   - Ejemplo: "Spring Boot 3.5 WebSocket STOMP configuration 2025"

2. **WebFetch** - Para leer páginas específicas de documentación
   - ✅ Después de encontrar la página correcta con WebSearch
   - ✅ Para leer guías oficiales completas
   - ✅ Para verificar firmas exactas de métodos

3. **Task agents** - Para investigaciones complejas y multi-step
   - ✅ Explorar arquitecturas grandes (subagent_type: Explore)
   - ✅ Investigaciones que requieren múltiples búsquedas
   - ✅ Análisis de seguridad o performance
   - ✅ Refactorings complejos multi-archivo

4. **Grep/Glob** - Para entender patrones existentes en el código
   - ✅ Buscar cómo se usa una anotación en el proyecto
   - ✅ Encontrar ejemplos de configuración existente
   - ✅ Verificar nomenclatura y convenciones del proyecto

5. **Read** - Para verificar implementación actual
   - ✅ SIEMPRE antes de modificar un archivo
   - ✅ Para entender contexto completo de un componente
   - ✅ Para verificar dependencias entre clases

**📋 CHECKLIST OBLIGATORIO ANTES DE ESCRIBIR CÓDIGO:**

```
[ ] 1. ¿Busqué en la documentación oficial de Spring Boot?
[ ] 2. ¿Usé WebSearch para encontrar best practices 2025?
[ ] 3. ¿Verifiqué si Spring Boot ya tiene esta funcionalidad nativa?
[ ] 4. ¿Busqué en el codebase actual patrones similares? (Grep/Glob)
[ ] 5. ¿Leí los archivos relacionados para entender el contexto? (Read)
[ ] 6. ¿Confirmé que mi enfoque sigue las recomendaciones oficiales?
[ ] 7. ¿Pregunté al usuario si tengo alguna duda? (AskUserQuestion)
```

**❌ PROHIBIDO - Atajos que llevan a código malo:**
- ❌ "Voy a implementar X porque creo que así funciona"
- ❌ "Este patrón funcionó en otro proyecto, lo voy a usar"
- ❌ "No necesito buscar docs, es obvio cómo se hace"
- ❌ "Inventaré una clase helper para esto"
- ❌ "Copiaré este código de Stack Overflow sin verificar"

**✅ CORRECTO - Enfoque profesional:**
- ✅ "Voy a buscar en docs oficiales cómo Spring Boot recomienda hacer X"
- ✅ "Usaré WebSearch para verificar best practices 2025 para X"
- ✅ "Voy a usar Grep para ver cómo se implementa X en este proyecto"
- ✅ "Crearé un Task agent para investigar a fondo este tema complejo"

### 3. Spring Boot Architecture Standards

**🏗️ SIGUE LA ARQUITECTURA RECOMENDADA POR SPRING BOOT - NO INVENTES LA TUYA 🏗️**

**REGLA FUNDAMENTAL**: Si Spring Boot recomienda un patrón, **ÚSALO**. No crees "tu propia versión mejorada".

**Arquitectura en capas (OBLIGATORIO):**
```
Controller (REST endpoints)
    ↓ (llama a)
Service (lógica de negocio, @Transactional)
    ↓ (llama a)
Repository (acceso a datos, extiende JpaRepository)
```

**Patrones Spring Boot que DEBES seguir:**

1. **Dependency Injection - Constructor Injection SIEMPRE**
   ```kotlin
   // ✅ CORRECTO - Constructor injection (inmutable, testeable)
   @Service
   class UserService(
       private val userRepository: UserRepository,
       private val emailService: EmailService
   ) { ... }

   // ❌ MAL - Field injection (mutable, difícil de testear)
   @Service
   class UserService {
       @Autowired
       lateinit var userRepository: UserRepository
   }
   ```

2. **Configuration - @ConfigurationProperties para configuraciones agrupadas**
   ```kotlin
   // ✅ CORRECTO - Type-safe configuration
   @ConfigurationProperties(prefix = "greenhouse")
   data class GreenhouseProperties(
       val simulation: SimulationConfig,
       val mqtt: MqttConfig
   )

   // ❌ MAL - @Value por todos lados
   @Value("\${greenhouse.simulation.enabled}")
   lateinit var simulationEnabled: String
   ```

3. **Events - ApplicationEvents para desacoplamiento**
   - ✅ Usa Spring Events para comunicación entre componentes
   - ✅ Ya implementado: `GreenhouseMessageEvent`
   - ❌ NO uses callbacks directos o coupling fuerte

4. **Transactions - @Transactional en capa de servicio**
   ```kotlin
   // ✅ CORRECTO - Transaction en servicio con propagation adecuada
   @Transactional("timescaleTransactionManager")
   fun processData(data: RealDataDto) { ... }

   // ❌ MAL - Sin transacción o en capa incorrecta
   fun processData(data: RealDataDto) { ... }
   ```

5. **Validation - Jakarta Bean Validation**
   ```kotlin
   // ✅ CORRECTO - Validación declarativa
   @PostMapping("/greenhouse")
   fun create(@Valid @RequestBody request: CreateGreenhouseRequest) { ... }

   // ❌ MAL - Validación manual
   fun create(request: CreateGreenhouseRequest) {
       if (request.name.isNullOrEmpty()) throw Exception("...")
   }
   ```

**🚀 APROVECHA TODO EL PODER DE SPRING BOOT:**

**ANTES de implementar algo manualmente, pregúntate:**
> "¿Spring Boot ya tiene esto?"

**Features de Spring Boot que DEBES usar:**

- ✅ **Auto-configuration**: Deja que Spring Boot configure lo que pueda
- ✅ **Actuator**: Para monitoring (`/actuator/health`, `/actuator/metrics`)
- ✅ **Profiles**: Para configuración por entorno (`@Profile("dev")`, `@Profile("prod")`)
- ✅ **Conditional Beans**: `@ConditionalOnProperty`, `@ConditionalOnMissingBean`, etc.
- ✅ **Starters**: Usa spring-boot-starter-* para dependencies
- ✅ **Properties**: Centraliza config en `application.yaml`
- ✅ **Events**: `ApplicationEventPublisher` para comunicación desacoplada
- ✅ **Scheduling**: `@Scheduled` para tareas programadas
- ✅ **Async**: `@Async` para operaciones no bloqueantes
- ✅ **Caching**: `@Cacheable`, `@CacheEvict` para caching declarativo

**Ejemplos de "reinventar la rueda" que DEBES evitar:**

❌ **MAL**: Crear `ConfigurationManager` custom cuando Spring Boot tiene `@ConfigurationProperties`
❌ **MAL**: Crear `TaskScheduler` custom cuando Spring Boot tiene `@Scheduled`
❌ **MAL**: Crear `CacheManager` custom cuando Spring Boot tiene `@Cacheable`
❌ **MAL**: Crear `EventBus` custom cuando Spring Boot tiene `ApplicationEventPublisher`
❌ **MAL**: Crear sistema de profiles custom cuando Spring Boot tiene `@Profile`

✅ **BIEN**: Usar las features nativas de Spring Boot y extenderlas solo si es necesario

### 4. Code Quality Standards (SOLID Principles)

**📖 CÓDIGO PARA HUMANOS, NO PARA MÁQUINAS 📖**

**REGLA ABSOLUTA**: El código se escribe UNA vez, pero se LEE cientos de veces. Prioriza LEGIBILIDAD sobre "eficiencia" o "brevedad".

**SOLID Principles - NON-NEGOTIABLE en CADA clase:**

**S - Single Responsibility (Una clase = Un propósito)**
- ✅ Cada clase hace UNA cosa y la hace bien
- ✅ Métodos hacen UNA cosa (< 50 líneas)
- ✅ Si una clase tiene "and" en su descripción, probablemente viola SRP
- ❌ NO crees clases "god object" que hacen todo
- ❌ NO pongas lógica de negocio en controladores
- ❌ NO mezcles responsabilidades (ej: validación + persistencia + notificación en un método)

**O - Open/Closed (Abierto para extensión, cerrado para modificación)**
- ✅ Usa interfaces y abstracciones
- ✅ Prefer composition over inheritance
- ✅ Diseña para que nuevas features no requieran modificar código existente
- ❌ NO uses inheritance para reuso de código (usa composition)

**L - Liskov Substitution (Los subtipos deben ser intercambiables)**
- ✅ Respeta contratos definidos por interfaces
- ✅ Los subtipos deben funcionar donde se usa el tipo base
- ❌ NO debilites precondiciones ni fortalezcas postcondiciones

**I - Interface Segregation (Interfaces pequeñas y específicas)**
- ✅ Crea interfaces focused y específicas para cada cliente
- ❌ NO crees interfaces "gordas" con métodos que muchos clientes no usan
- ❌ NO fuerces a clientes a implementar métodos que no necesitan

**D - Dependency Inversion (Depende de abstracciones, no concreciones)**
- ✅ Usa interfaces, no implementaciones concretas
- ✅ Spring DI container maneja las dependencias
- ✅ Constructor injection SIEMPRE (inmutable, testeable)
- ❌ NO uses `new` para crear dependencias
- ❌ NO uses field injection (`@Autowired` en properties)

---

**🔍 LEGIBILIDAD - MÁS IMPORTANTE QUE BREVEDAD 🔍**

**El código debe ser auto-documentado. Un desarrollador nuevo debe entender QUÉ hace mirándolo.**

**Comparación CÓDIGO HUMANO vs CÓDIGO MÁQUINA:**

```kotlin
// ✅ EXCELENTE: Código humano - legible, debuggeable, mantenible
@Service
class GreenhouseDataService(
    private val cacheService: GreenhouseCacheService,
    private val sensorRepository: SensorReadingRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional("timescaleTransactionManager")
    fun processAndCacheSensorReading(sensorData: RealDataDto): SensorReading {
        logger.debug("Processing sensor reading for greenhouse: ${sensorData.greenhouseId}")

        // Cache the raw message for quick retrieval
        val cachedMessage = cacheService.cacheMessage(sensorData)

        // Transform and persist to time-series database
        val sensorReading = sensorData.toSensorReading()
        val savedReading = sensorRepository.save(sensorReading)

        // Notify interested parties via event
        val event = GreenhouseMessageEvent(source = this, message = sensorData)
        eventPublisher.publishEvent(event)

        logger.info("Successfully processed sensor reading with ID: ${savedReading.id}")
        return savedReading
    }
}

// ❌ TERRIBLE: Código máquina - críptico, imposible de debuggear
@Service
class GDS(private val cs: GCS, private val r: SRR, private val ep: AEP) {
    fun p(d: RDD) = r.save(d.toSR()).also { cs.c(d); ep.pub(GME(this, d)) }
}
```

**¿Por qué el primer ejemplo es MUCHO mejor?**

1. **Nombres descriptivos**: `GreenhouseDataService` vs `GDS`
2. **Variables con significado**: `sensorData` vs `d`, `cachedMessage` vs resultado inline
3. **Comentarios explicativos**: Cada paso explica el "por qué"
4. **Logging contextual**: Para debugging en producción
5. **Separación de pasos**: Cada operación en su propia línea
6. **Métodos cohesivos**: < 50 líneas, un solo nivel de abstracción
7. **Anotaciones claras**: `@Transactional` con qualifier específico

**📏 REGLAS DE LEGIBILIDAD (OBLIGATORIAS):**

1. **Nombres de variables - Explican QUÉ son, no solo su tipo**
   ```kotlin
   // ✅ BIEN: Nombres descriptivos
   val totalSensorReadings = readings.size
   val isSimulationEnabled = config.simulation.enabled
   val greenhouseTemperatureCelsius = data.temperaturaInvernadero01

   // ❌ MAL: Nombres crípticos o solo tipo
   val count = readings.size
   val flag = config.simulation.enabled
   val temp = data.temperaturaInvernadero01
   ```

2. **Métodos - Nombres verbales que explican QUÉ hacen**
   ```kotlin
   // ✅ BIEN: Verbos descriptivos
   fun calculateAverageTemperature(readings: List<SensorReading>): Double
   fun validateGreenhouseConfiguration(config: GreenhouseConfig)
   fun publishSensorDataToWebSocket(data: RealDataDto)

   // ❌ MAL: Nombres ambiguos
   fun process(data: Any)
   fun handle(config: Any)
   fun doIt()
   ```

3. **Métodos cortos - < 50 líneas, UN nivel de abstracción**
   ```kotlin
   // ✅ BIEN: Método corto, enfocado, fácil de entender
   fun processGreenhouseData(payload: String, greenhouseId: String) {
       val data = parsePayload(payload, greenhouseId)
       cacheData(data)
       persistToDatabase(data)
       notifyClients(data)
   }

   // ❌ MAL: Método de 200 líneas con toda la lógica inline
   fun processGreenhouseData(payload: String, greenhouseId: String) {
       // ... 200 líneas de parsing, validación, cache, DB, websocket ...
   }
   ```

4. **Error messages - Contextuales y accionables**
   ```kotlin
   // ✅ BIEN: Mensaje con contexto y cómo resolver
   throw IllegalArgumentException(
       "Failed to parse sensor data for greenhouse '$greenhouseId'. " +
       "Expected JSON with 22 fields, but received: ${payload.take(100)}..."
   )

   // ❌ MAL: Mensaje genérico sin contexto
   throw Exception("Error")
   ```

5. **Logging - Niveles apropiados con contexto**
   ```kotlin
   // ✅ BIEN: Logging contextual en niveles apropiados
   logger.debug("Received MQTT message from topic: $topic, size: ${payload.length} bytes")
   logger.info("Successfully processed greenhouse data for ID: $greenhouseId")
   logger.warn("Sensor reading outside normal range: temp=${temp}°C (expected 15-30°C)")
   logger.error("Failed to save to database for greenhouse $greenhouseId", exception)

   // ❌ MAL: Logging sin contexto o nivel incorrecto
   logger.info("Data: $data") // Demasiado detalle para INFO
   logger.error("Error") // Sin contexto ni excepción
   ```

6. **Comentarios - Para el "POR QUÉ", no el "QUÉ"**
   ```kotlin
   // ✅ BIEN: Explica POR QUÉ se hace algo no obvio
   // Use TreeMap to maintain sensor readings in chronological order
   // This is critical for time-series aggregations in the UI
   val sortedReadings = TreeMap<Instant, SensorReading>()

   // ❌ MAL: Explica QUÉ hace el código (obvio al leerlo)
   // Create a map
   val map = HashMap()
   ```

**🐛 DEBUGGING & MAINTAINABILITY:**

**Pregúntate SIEMPRE:**
- ¿Podré entender este código en 6 meses?
- ¿Un nuevo developer puede debuggearlo sin preguntarme?
- ¿Los logs me darán suficiente información en producción?
- ¿Los nombres de variables explican su propósito?
- ¿Los error messages guían hacia la solución?

**Si la respuesta a alguna es "no" → REFACTORIZA**

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

**❓ CUANDO TENGAS DUDAS - PREGUNTA, NO ASUMAS ❓**

**REGLA DE ORO**: Si algo no está 100% claro, USA `AskUserQuestion` tool. **SIEMPRE**.

**No adivines. No interpretes. No asumas. PREGUNTA.**

**Situaciones donde DEBES preguntar (OBLIGATORIO):**

1. **Requisitos ambiguos o incompletos**
   ```
   Usuario dice: "Añade validación a los datos"
   ❌ MAL: Asumir qué validar
   ✅ BIEN: Preguntar "¿Qué tipo de validación necesitas? (rango de valores, formato, obligatoriedad, etc.)"
   ```

2. **Múltiples enfoques válidos**
   ```
   Usuario dice: "Implementa caché para los datos"
   ❌ MAL: Elegir arbitrariamente Redis/In-Memory/Database
   ✅ BIEN: Preguntar "¿Qué tipo de caché prefieres? (Redis distribuido, Caffeine in-memory, DB cache)"
   ```

3. **Decisiones arquitectónicas**
   ```
   Usuario dice: "Procesa los mensajes"
   ❌ MAL: Asumir sincrónico o asincrónico
   ✅ BIEN: Preguntar "¿Procesamiento sincrónico (bloquea el thread) o asincrónico (@Async)?"
   ```

4. **Trade-offs de performance vs simplicidad**
   ```
   Usuario dice: "Optimiza la búsqueda"
   ❌ MAL: Implementar caché complejo sin preguntar
   ✅ BIEN: Preguntar "¿Priorizamos velocidad (caché, índices) o simplicidad (queries directas)?"
   ```

5. **Scope de implementación**
   ```
   Usuario dice: "Añade autenticación"
   ❌ MAL: Asumir JWT/OAuth/Basic/etc.
   ✅ BIEN: Preguntar "¿Qué método de autenticación? (JWT, OAuth2, Session-based, API keys)"
   ```

6. **Librerías o dependencias**
   ```
   Usuario dice: "Implementa logging estructurado"
   ❌ MAL: Elegir arbitrariamente Logback/Log4j2/etc.
   ✅ BIEN: Preguntar "¿Qué librería de logging prefieres? (Logback JSON, Log4j2, Logstash)"
   ```

**Formato de preguntas efectivas:**

Usa `AskUserQuestion` con opciones claras y trade-offs:

```kotlin
// ✅ EXCELENTE: Pregunta con opciones y trade-offs
"¿Cómo quieres manejar errores en el procesamiento MQTT?"

Opciones:
1. Fail fast - Lanza excepción y detiene procesamiento
   + Más seguro, no procesa datos corruptos
   - Puede perder mensajes si hay errores transitorios

2. Retry con exponential backoff
   + Tolera errores transitorios
   - Más complejo, puede retrasar procesamiento

3. Dead Letter Queue
   + No pierde mensajes, permite análisis posterior
   - Requiere infraestructura adicional (DLQ)
```

**Ejemplos REALES de cuándo preguntar:**

✅ **BIEN**: "Los datos vienen cada 5 segundos. ¿Debo cachear TODAS las lecturas o solo las últimas N? Esto afecta el uso de memoria."

✅ **BIEN**: "¿Esta API debe ser pública (con rate limiting) o interna (sin autenticación)?"

✅ **BIEN**: "¿Validamos los datos ANTES de guardar (puede rechazar mensajes) o DESPUÉS (permite análisis de datos inválidos)?"

✅ **BIEN**: "¿Prefieres una configuración type-safe (@ConfigurationProperties) o más flexible (@Value con defaults)?"

❌ **MAL**: Implementar y luego decir "Asumí que querías X"

❌ **MAL**: Elegir opción A porque "me pareció mejor"

❌ **MAL**: "No estaba claro, así que lo hice como creí conveniente"

**⚠️ PROHIBIDO - No hagas NINGUNA de estas cosas:**

❌ Implementar basándote en "supongo que el usuario quiere..."
❌ Elegir arbitrariamente entre opciones válidas sin preguntar
❌ Dejar decisiones importantes a "tu interpretación"
❌ Asumir requisitos no funcionales (performance, seguridad, escalabilidad)
❌ Implementar features "porque son buenas prácticas" sin confirmar que el usuario las quiere

**✅ CORRECTO - Enfoque profesional:**

1. **Identifica la ambigüedad** temprano
2. **Usa AskUserQuestion** con opciones claras
3. **Explica trade-offs** de cada opción
4. **Espera confirmación** antes de implementar
5. **Implementa exactamente** lo que el usuario confirma

### 7. Continuous Improvement

**💡 SÉ PROACTIVO - SIEMPRE PROPÓN MEJORAS 💡**

**Tu trabajo NO es solo "hacer lo que te piden". Tu trabajo es hacer el código MEJOR.**

**OBLIGATORIO: Cuando veas problemas, DILO. No los ignores.**

**Problemas que DEBES identificar y reportar SIEMPRE:**

1. **Violaciones de SOLID Principles**
   ```
   ❌ Encuentras: Una clase con 5 responsabilidades diferentes
   ✅ Debes decir: "Esta clase viola Single Responsibility. Sugiero separarla en:
      - UserValidator (validación)
      - UserPersistence (DB operations)
      - UserNotifier (emails/notifications)"
   ```

2. **Código duplicado (DRY violations)**
   ```
   ❌ Encuentras: Mismo código en 3 lugares diferentes
   ✅ Debes decir: "Detecté duplicación del código de validación en UserController,
      ProductController y OrderController. Sugiero crear un ValidationService
      centralizado."
   ```

3. **Missing error handling**
   ```
   ❌ Encuentras: Método sin try-catch ni validación
   ✅ Debes decir: "Este método puede lanzar NullPointerException si X es null.
      Sugiero añadir validación con Jakarta Bean Validation (@NotNull) o Elvis operator."
   ```

4. **Performance bottlenecks**
   ```
   ❌ Encuentras: Query N+1, loops anidados O(n²), falta de índices
   ✅ Debes decir: "Este código tiene un problema N+1 con lazy loading. Cada sensor
      genera una query. Sugiero usar @EntityGraph o JOIN FETCH para cargar todo
      en una query."
   ```

5. **Security vulnerabilities**
   ```
   ❌ Encuentras: SQL injection, XSS, hardcoded passwords, datos sensibles en logs
   ✅ Debes decir: "⚠️ VULNERABILIDAD: Este código es vulnerable a SQL injection.
      NUNCA concatenes strings en queries. Usa @Query con parámetros named."
   ```

6. **Missing tests**
   ```
   ❌ Encuentras: Lógica crítica sin tests
   ✅ Debes decir: "Esta lógica de cálculo de temperatura promedio es crítica y no
      tiene tests. Sugiero añadir unit tests con JUnit 5 + MockK."
   ```

7. **Outdated dependencies con vulnerabilidades**
   ```
   ❌ Encuentras: Librería con CVEs conocidos
   ✅ Debes decir: "⚠️ La versión X de [librería] tiene vulnerabilidad CVE-2024-XXXX.
      Spring Boot 3.5.7 ya incluye versión Y parcheada. Sugiero actualizar."
   ```

8. **Código custom cuando Spring Boot lo proporciona**
   ```
   ❌ Encuentras: EventBus custom, Scheduler custom, Cache custom
   ✅ Debes decir: "Estás implementando un EventBus custom. Spring Boot ya tiene
      ApplicationEventPublisher. Sugiero reemplazarlo por la solución nativa."
   ```

9. **Configuración hardcoded que debería ser externalizada**
   ```
   ❌ Encuentras: URLs, timeouts, limits en código
   ✅ Debes decir: "Estas URLs están hardcoded. Deberían estar en application.yaml
      con @ConfigurationProperties para poder cambiarlas sin recompilar."
   ```

10. **Nombres poco claros o misleading**
    ```
    ❌ Encuentras: Clase "Manager", "Helper", "Util", método "process", "handle"
    ✅ Debes decir: "El nombre 'DataManager' es vago. Basándome en lo que hace,
       sugiero renombrar a 'SensorReadingTransformer' para mayor claridad."
    ```

**Formato de sugerencias efectivas:**

```
🔍 Problema identificado: [Descripción clara del problema]

❌ Código actual:
[Snippet del código problemático]

✅ Solución propuesta:
[Snippet del código mejorado]

📊 Beneficios:
- [Beneficio 1]
- [Beneficio 2]

⚠️ Trade-offs (si los hay):
- [Consideración 1]

💬 ¿Quieres que implemente esta mejora?
```

**Ejemplos REALES de mejoras proactivas:**

✅ **EXCELENTE**: "Noté que usas `@Value` para configuración MQTT. Spring Boot recomienda `@ConfigurationProperties` para configuraciones agrupadas. ¿Quieres que refactorice a un `MqttProperties` type-safe?"

✅ **EXCELENTE**: "Este loop itera sobre 10,000 sensores y hace una query por cada uno (N+1 problem). Sugiero usar batch loading con `repository.findAllById()` para reducir de 10,000 queries a 1. ¿Procedo?"

✅ **EXCELENTE**: "La clase `GreenhouseService` tiene 800 líneas y maneja validación, persistencia, caché y notificaciones. Viola Single Responsibility. Sugiero separar en 4 servicios especializados. ¿Quieres un plan detallado?"

✅ **EXCELENTE**: "⚠️ SEGURIDAD: Los passwords se loggean en línea 45. Esto es un riesgo de seguridad. Sugiero remover ese log o usar masking (`password=***`). ¿Cuál prefieres?"

**🚀 MENTALIDAD PROACTIVA:**

No esperes a que te pidan mejoras. **OFRÉCELAS**.

- Cuando veas código malo → Señálalo y propón solución
- Cuando veas patrones antiguos → Sugiere alternativas modernas de Spring Boot
- Cuando veas oportunidades de optimización → Proponlas con métricas
- Cuando veas riesgos de seguridad → ALERTALOS inmediatamente

**Tu objetivo: Dejar el código MEJOR de cómo lo encontraste**

### 8. Time Management & Task Planning

**⏰ CALIDAD SOBRE VELOCIDAD - SIEMPRE ⏰**

**REGLA ABSOLUTA: NO HAY PRISA. Hazlo BIEN, no rápido.**

Código apurado = Código malo = Más tiempo corrigiendo bugs después

**🎯 Tómate TODO el tiempo que necesites para:**

1. **Investigar** en documentación oficial
2. **Planificar** la implementación correctamente
3. **Preguntar** cuando tengas dudas
4. **Implementar** con SOLID principles
5. **Revisar** tu propio código
6. **Documentar** cambios importantes

**Herramientas OBLIGATORIAS para tareas complejas:**

**1. TodoWrite - Para planificar y trackear progreso**

✅ **Úsalo SIEMPRE que una tarea tenga 3+ pasos**

```
Ejemplo de cuándo usar TodoWrite:

Usuario pide: "Implementa autenticación JWT"

Debes crear TODO list:
[ ] 1. Investigar Spring Security 3.5 JWT configuration 2025
[ ] 2. Añadir dependencias (spring-boot-starter-security, jjwt)
[ ] 3. Crear JwtTokenProvider service
[ ] 4. Configurar SecurityFilterChain
[ ] 5. Implementar JwtAuthenticationFilter
[ ] 6. Añadir endpoints de login/register
[ ] 7. Testear con Postman/curl
[ ] 8. Documentar en README
```

**Beneficios de TodoWrite:**
- El usuario ve tu progreso en tiempo real
- No olvidas pasos importantes
- Puedes retomar si algo falla
- Demuestra profesionalismo y organización

**2. Task Agents - Para investigación y refactoring complejo**

✅ **Créalos sin dudar para tareas que requieren:**

- **Exploración de codebase grande**
  ```
  Ejemplo: "Entender cómo funciona el sistema de caché en este proyecto"
  → Crea Task agent (subagent_type: Explore)
  ```

- **Investigación multi-step**
  ```
  Ejemplo: "Investigar mejores prácticas 2025 para WebSocket con STOMP"
  → Crea Task agent para research completo
  ```

- **Refactoring multi-archivo**
  ```
  Ejemplo: "Refactorizar validación dispersa en 15 archivos a un servicio centralizado"
  → Crea Task agent para análisis y ejecución
  ```

- **Análisis de seguridad o performance**
  ```
  Ejemplo: "Analizar posibles vulnerabilidades SQL injection en queries"
  → Crea Task agent especializado
  ```

**Proceso para tareas complejas (SIGUE ESTE ORDEN):**

```
1️⃣ PLAN - Crear TODO list con TodoWrite
   ↓
2️⃣ RESEARCH - Usar WebSearch/Task agents para investigar
   ↓
3️⃣ ASK - Clarificar ambigüedades con AskUserQuestion
   ↓
4️⃣ IMPLEMENT - Escribir código SOLID, legible, testeado
   ↓
5️⃣ REVIEW - Auto-revisar contra principios de este CLAUDE.md
   ↓
6️⃣ DOCUMENT - Actualizar docs relevantes (README, CLAUDE.md, etc.)
   ↓
7️⃣ IMPROVE - Proponer mejoras adicionales si ves oportunidades
```

**❌ NUNCA hagas esto:**

❌ Implementar sin planificar (saltar directo a código)
❌ Asumir que "es simple" y no investigar
❌ Apurarte para "terminar rápido"
❌ Saltarte pasos porque "no son necesarios"
❌ No usar TodoWrite en tareas multi-step
❌ No crear Task agents cuando la tarea es compleja

**✅ SIEMPRE haz esto:**

✅ Planifica con TodoWrite si hay 3+ pasos
✅ Investiga en docs oficiales ANTES de codear
✅ Pregunta si tienes la MÁS MÍNIMA duda
✅ Crea Task agents para tareas complejas
✅ Revisa tu código contra SOLID principles
✅ Actualiza documentación relevante
✅ Propón mejoras proactivamente

**Ejemplos de uso correcto:**

```
✅ EXCELENTE - Tarea compleja bien manejada:

Usuario: "Implementa sistema de notificaciones push"

Claude responde:
"Voy a planificar esta implementación compleja. Primero crearé un TODO list
y luego investigaré best practices 2025 para push notifications con Spring Boot.

[Crea TodoWrite con 8 pasos]
[Crea Task agent para investigar Spring Boot push notification patterns]
[Usa WebSearch para "Spring Boot 3.5 push notifications 2025"]
[Pregunta al usuario: ¿Firebase, OneSignal, o WebPush nativo?]
[Implementa según la respuesta]
[Revisa código contra SOLID]
[Actualiza docs]"
```

```
❌ MAL - Apurado y sin planificación:

Usuario: "Implementa sistema de notificaciones push"

Claude responde:
"Ok, voy a añadir Firebase y un endpoint para enviar notificaciones"
[Escribe código sin investigar]
[No pregunta qué servicio usar]
[No planifica]
[Código no sigue SOLID]
```

**🎯 MENTALIDAD CORRECTA:**

**"¿Cuánto tiempo toma implementar X?"**

Respuesta incorrecta ❌: "5 minutos, es simple"
Respuesta correcta ✅: "Déjame investigar best practices, planificar los pasos y asegurarme de hacerlo bien. El tiempo que tome será el necesario para hacerlo correctamente."

**Recuerda**: El usuario NO quiere código rápido. Quiere código **CORRECTO, MANTENIBLE y ESCALABLE**.

**Tómate el tiempo. Hazlo bien. Siempre.**

### 9. Context Awareness

**📅 ESTAMOS EN 2025 - USA INFORMACIÓN ACTUALIZADA 📅**

**CRÍTICO**: El año actual es **2025**. NO uses documentación o ejemplos de 2020-2023.

**Implicaciones para búsquedas y documentación:**

1. **WebSearch - SIEMPRE incluye "2025" o la versión actual**
   ```
   ❌ MAL: "Spring Boot WebSocket configuration"
   ✅ BIEN: "Spring Boot 3.5 WebSocket configuration 2025"

   ❌ MAL: "Kotlin best practices"
   ✅ BIEN: "Kotlin 2.0+ best practices 2025"
   ```

2. **Versiones y APIs - Verifica que NO estén deprecadas**
   ```
   ⚠️ ALERTA: Muchas APIs cambiaron entre Spring Boot 2.x y 3.x

   Ejemplos de cambios importantes:
   - javax.* → jakarta.* (Jakarta EE 9+)
   - WebSecurityConfigurerAdapter → SecurityFilterChain (deprecado en 2.7)
   - Properties migradas en Spring Boot 3.x

   → SIEMPRE verifica migration guides antes de usar ejemplos antiguos
   ```

3. **Stack Overflow y blogs - Verifica la fecha**
   ```
   ❌ Respuesta de Stack Overflow de 2019 → Probablemente OBSOLETA
   ✅ Respuesta de 2024-2025 o docs oficiales → ACTUAL

   SIEMPRE pregúntate: "¿Esto aplica a Spring Boot 3.5.7?"
   ```

**Versiones específicas de este proyecto:**

- **Spring Boot**: 3.5.7 (Spring Framework 6.x)
  - ⚠️ Esta es una versión MODERNA (2025)
  - Usa Jakarta EE 10+, NO javax.*
  - SecurityFilterChain, NO WebSecurityConfigurerAdapter
  - Native Compilation ready (GraalVM support)

- **Java**: 21 LTS
  - ✅ Virtual Threads disponibles (Project Loom)
  - ✅ Pattern Matching for switch
  - ✅ Record patterns
  - ✅ Sequenced Collections
  - → USA estas features modernas cuando sea apropiado

- **Kotlin**: 2.2.21
  - ✅ K2 compiler (mucho más rápido)
  - ✅ Context receivers
  - ✅ Data objects
  - → Aprovecha features modernas de Kotlin

- **PostgreSQL**: 16
  - ✅ JSON improvements
  - ✅ Performance improvements
  - → Puedes usar features de Postgres 16

- **Redis**: 7
  - ✅ Redis Functions
  - ✅ ACLs v2
  - → Puedes usar features modernas de Redis

**Migration Guides que DEBES conocer:**

Si encuentras código antiguo o ejemplos de versiones anteriores:

1. **Spring Boot 2.x → 3.x Migration**
   - javax.* → jakarta.*
   - WebSecurityConfigurerAdapter removal
   - Property changes
   - → https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide

2. **Spring Security 5.x → 6.x**
   - Lambda DSL obligatorio
   - SecurityFilterChain pattern
   - → Usa docs 2025, no ejemplos antiguos

**⚠️ RED FLAGS - Indicadores de código/docs OBSOLETOS:**

❌ "extends WebSecurityConfigurerAdapter" → DEPRECADO desde Spring Boot 2.7
❌ "import javax.persistence.*" → Cambió a jakarta.persistence en Spring Boot 3.x
❌ "import javax.validation.*" → Cambió a jakarta.validation
❌ Ejemplos de Spring Boot 2.x sin aclarar que aplican a 3.x
❌ Artículos de blogs de 2020-2022 (probablemente obsoletos)

✅ "SecurityFilterChain" → Patrón ACTUAL
✅ "import jakarta.*" → Correcto para Spring Boot 3.x
✅ Docs oficiales de Spring Boot 3.5+ → ACTUALES
✅ Ejemplos con año 2024-2025 → Probablemente válidos

**Flujo de verificación de información:**

```
1. Encuentro solución/ejemplo
   ↓
2. ¿Es de docs oficiales de Spring Boot 3.5+?
   SÍ → Probablemente válido
   NO → Continuar verificación
   ↓
3. ¿De qué año es?
   2024-2025 → Revisar compatibilidad
   2020-2023 → ⚠️ VERIFICAR EXTENSIVAMENTE antes de usar
   ↓
4. ¿Usa javax.* o jakarta.*?
   jakarta.* → Correcto para Spring Boot 3.x
   javax.* → ⚠️ OBSOLETO, necesita migración
   ↓
5. ¿WebSearch confirma que sigue siendo best practice en 2025?
   SÍ → OK para usar
   NO → Buscar alternativa moderna
```

**Cuando NO estés seguro:**

Si encuentras un ejemplo y no estás 100% seguro de que aplica a Spring Boot 3.5.7 (2025):

1. ✅ Usa WebSearch: "Spring Boot 3.5 [feature] 2025 best practice"
2. ✅ Verifica en docs oficiales de Spring Boot 3.5+
3. ✅ Pregunta al usuario si tienes dudas

**NUNCA asumas que un patrón de 2020-2022 sigue siendo válido en 2025 sin verificar.**

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
│   ├── GreenhouseCacheService.kt      - Redis operations (Sorted Set)
│   └── GreenhouseDataSimulator.kt     - Generates realistic simulated sensor data
│
├── scheduler/
│   └── GreenhouseSimulationScheduler.kt - Scheduled task for simulation mode (every 5s)
│
├── entities/
│   ├── requests/
│   │   └── ... (Request DTOs)
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

### Simulation Mode (Development Feature)

**Purpose**: Generate realistic sensor data when physical greenhouse sensors are unavailable.

**Configuration** (`application.yaml`):
```yaml
greenhouse:
  simulation:
    enabled: true          # Enable/disable simulation mode
    greenhouse-id: "001"   # Default greenhouse ID for simulated data
    interval: 5000         # Interval in milliseconds (5 seconds)
```

**Components**:
- **GreenhouseDataSimulator** (`service/GreenhouseDataSimulator.kt`):
  - Generates realistic RealDataDto with 22 fields
  - Temperature ranges: 15-30°C with gradual variations
  - Humidity ranges: 40-80% with gradual variations
  - Sector/Extractor values: Random but realistic
  - Uses previous values for smooth transitions (not completely random)

- **GreenhouseSimulationScheduler** (`scheduler/GreenhouseSimulationScheduler.kt`):
  - Scheduled task annotated with `@Scheduled(fixedDelayString = "\${greenhouse.simulation.interval}")`
  - Conditional execution: `@ConditionalOnProperty("greenhouse.simulation.enabled", havingValue = "true")`
  - Generates data every 5 seconds and processes through normal MQTT pipeline

**How It Works**:
1. Scheduler triggers every 5 seconds (configurable)
2. Simulator generates realistic RealDataDto
3. Data is processed through `MqttMessageProcessor` (same as real MQTT data)
4. Cached in Redis, saved to TimescaleDB, broadcast via WebSocket
5. **WARNING** log displayed: "⚠️ SIMULATION MODE ENABLED - Generating fake greenhouse data"

**When to Use**:
- Local development without access to physical sensors
- Testing WebSocket/database integration
- Load testing with predictable data patterns
- Demonstrations and training

**IMPORTANT**: Always disable in production by setting `greenhouse.simulation.enabled: false`

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
@Transactional("timescaleTransactionManager")
fun processGreenhouseData(payload: String, greenhouseId: String) {
    // 1. Parse JSON to RealDataDto (22 fields)
    val messageDto = payload.toRealDataDto(Instant.now(), greenhouseId)

    // 2. Cache in Redis (sorted set)
    cacheService.cacheMessage(messageDto)

    // 3. Transform to multiple SensorReading entities
    val readings = mutableListOf<SensorReading>()
    val jsonMap = objectMapper.readValue<Map<String, Any?>>(payload)

    // Iterate over each JSON field and create SensorReading entity
    jsonMap.forEach { (key, value) ->
        if (value is Number) {
            readings.add(SensorReading(
                time = messageDto.timestamp,
                sensorId = key,
                greenhouseId = greenhouseId,
                sensorType = determineSensorType(key),  // "SENSOR", "SETPOINT", "UNKNOWN"
                value = value.toDouble(),
                unit = determineUnit(key)  // "°C", "%", "hPa", "value", "unit"
            ))
        }
    }

    // 4. Batch save to TimescaleDB (22 fields → 22 rows)
    repository.saveAll(readings)

    // 5. Publish Spring event (async, decoupled from MQTT)
    publisher.publishEvent(GreenhouseMessageEvent(this, messageDto))

    // 6. Echo to MQTT response topic (for verification)
    mqttPublishService.publishToResponseTopic(messageDto.toJson())
}
```

**Data Transformation Details**:
- **Input**: Single JSON payload with 22 fields
- **Output**: 22 `SensorReading` entities (one per field)
- **Unit Detection** (`determineUnit()` helper):
  - Temperature fields → "°C"
  - Humidity fields → "%"
  - Pressure fields → "hPa"
  - Others → "value" or "unit"
- **Sensor Type Classification** (`determineSensorType()` helper):
  - Fields containing "SETPOINT" → "SETPOINT"
  - Numeric sensor fields → "SENSOR"
  - Others → "UNKNOWN"

### MQTT Response/Echo Feature

**Purpose**: Verify data reception by echoing received messages back to MQTT broker.

**Implementation** (GreenhouseDataListener.kt):
```kotlin
fun handleGreenhouseData(message: Message<*>) {
    val payload = String(message.payload as ByteArray)

    // Process the data
    mqttMessageProcessor.processGreenhouseData(payload, greenhouseId)

    // Echo to response topic for verification
    mqttPublishService.publishToResponseTopic(payload)
}
```

**MQTT Topics**:
- **Inbound**: `GREENHOUSE` (receives sensor data)
- **Outbound**: `GREENHOUSE/RESPONSE` (echoes processed data)

**Use Cases**:
- Testing MQTT connectivity
- Verifying data format and parsing
- Debugging sensor integration issues

### WebSocket Broadcasting (GreenhouseWebSocketHandler.kt:39-55)

```kotlin
@EventListener
fun handleGreenhouseMessage(event: GreenhouseMessageEvent) {
    messagingTemplate.convertAndSend(
        "/topic/greenhouse/messages",
        event.message  // RealDataDto sent to clients (22 fields)
    )
}
```

**Key Points**:
- Uses Spring's `@EventListener` for decoupling
- Broadcasts RealDataDto (NOT individual SensorReading entities)
- Mobile/web clients receive complete sensor snapshot (all 22 fields at once)
- Async processing prevents MQTT blocking

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
- **SIMULATION_GUIDE.md** - Simulation mode configuration and usage guide

## Common Gotchas

1. **Dual DataSource**: Always specify `@Qualifier` when injecting repositories or transaction managers
2. **MQTT Topics**: Topic `GREENHOUSE` is for inbound data, `GREENHOUSE/RESPONSE` is for echoing back to broker
3. **WebSocket vs MQTT**: Mobile apps receive data via WebSocket (STOMP), not directly from MQTT
4. **DTO Format**: System currently uses `RealDataDto` (22 fields), not `GreenhouseMessageDto`
5. **Redis Score**: Sorted set uses `timestamp.toEpochMilli()` as score for time-based queries
6. **Batch Inserts**: Use `repository.saveAll()` for TimescaleDB to optimize bulk inserts (1 message → 22 SensorReading entities)
7. **JSON Mapping Inconsistency**: RealDataDto uses MIXED key formats (INTENTIONAL to match hardware):
   - Temperature/Humidity: **SPACES** (`"TEMPERATURA INVERNADERO 01"`)
   - Sectors/Extractors: **UNDERSCORES** (`"INVERNADERO_01_SECTOR_01"`)
   - This matches the actual greenhouse system's data format
8. **Simulation Mode**: Check `greenhouse.simulation.enabled` in application.yaml - if enabled, the system generates fake data instead of relying on real sensors. Always disable in production!
9. **Data Transformation**: One RealDataDto becomes multiple SensorReading entities. Don't expect a 1-to-1 mapping between DTO and entity.

## Kubernetes Deployment

### Structure Overview

The project is deployed on Kubernetes with a comprehensive multi-tier architecture located in the parent directory (`../`):

```
/home/admin/companies/apptolast/invernaderos/k8s/
├── 00-namespace.yaml              # Namespace: apptolast-invernadero-api
├── 01-secrets.yaml                # Secrets for DB passwords, MQTT credentials
├── 02-configmaps/                 # ConfigMaps for application.yaml
├── 03-storage/                    # PersistentVolumes and PersistentVolumeClaims
│   ├── pv-timescaledb.yaml
│   ├── pv-postgresql-metadata.yaml
│   └── pv-redis.yaml
├── 04-timescaledb/                # TimescaleDB StatefulSet + Service
│   ├── statefulset.yaml           # Port 30432 (NodePort)
│   └── service.yaml
├── 05-postgresql-metadata/        # PostgreSQL StatefulSet + Service
│   ├── statefulset.yaml           # Port 30433 (NodePort)
│   └── service.yaml
├── 06-redis/                      # Redis Deployment + Service
│   ├── deployment.yaml
│   └── service.yaml
├── 07-monitoring/                 # Prometheus + Grafana (optional)
├── 08-mqtt/                       # EMQX MQTT Broker
│   ├── deployment.yaml
│   └── service.yaml
├── 09-cronjobs/                   # Maintenance CronJobs
├── 10-api-prod/                   # InvernaderosAPI Production
│   ├── deployment.yaml
│   └── service.yaml
├── 11-api-dev/                    # InvernaderosAPI Development
│   ├── deployment.yaml
│   └── service.yaml
├── deploy.sh                      # Automated deployment script
├── undeploy.sh                    # Cleanup script
└── InvernaderosAPI/               # Spring Boot source code (this directory)
```

### Deployment Scripts

**deploy.sh** - Automated deployment in phases:
1. **Phase 1**: Namespace creation
2. **Phase 2**: Secrets (database passwords, MQTT credentials)
3. **Phase 3**: ConfigMaps (application configuration)
4. **Phase 4**: Storage (PV + PVC for data persistence)
5. **Phase 5**: TimescaleDB StatefulSet
6. **Phase 6**: PostgreSQL Metadata StatefulSet
7. **Phase 7**: Redis
8. **Phase 8**: Monitoring (Prometheus + Grafana)
9. **Phase 9**: EMQX MQTT Broker
10. **Phase 10**: CronJobs (backup, cleanup)
11. **Phase 11**: API Production
12. **Phase 12**: API Development

```bash
# Deploy entire stack
../deploy.sh

# Undeploy everything
../undeploy.sh
```

### Storage Configuration

**Host Paths** (on Kubernetes node):
```bash
/mnt/k8s-storage/invernaderos/
├── timescaledb/        # TimescaleDB data (chown 999:999)
├── postgresql-metadata/ # PostgreSQL data (chown 999:999)
└── redis/              # Redis data (chown 1000:1000)
```

**Setup commands** (run on K8s node before deployment):
```bash
sudo mkdir -p /mnt/k8s-storage/invernaderos/{timescaledb,postgresql-metadata,redis}
sudo chown -R 999:999 /mnt/k8s-storage/invernaderos/{timescaledb,postgresql-metadata}
sudo chown -R 1000:1000 /mnt/k8s-storage/invernaderos/redis
sudo chmod -R 755 /mnt/k8s-storage/invernaderos
```

### Database Access

**TimescaleDB** (time-series data):
- **Service**: `timescaledb-service.apptolast-invernadero-api.svc.cluster.local`
- **NodePort**: `30432` (external access)
- **Internal Port**: `5432`
- **Database**: `postgres`
- **Schema**: `public`
- **Table**: `sensor_readings`

**PostgreSQL Metadata**:
- **Service**: `postgresql-metadata-service.apptolast-invernadero-api.svc.cluster.local`
- **NodePort**: `30433` (external access)
- **Internal Port**: `5432`
- **Database**: `postgres`
- **Schema**: `metadata`
- **Tables**: `tenants`, `greenhouses`, `sensors`, `actuators`, `users`, `alerts`

**Redis** (cache):
- **Service**: `redis-service.apptolast-invernadero-api.svc.cluster.local`
- **Internal Port**: `6379`
- **Data Structure**: Sorted Set (`greenhouse:messages`)

### Environment-Specific Deployments

**Production** (`../10-api-prod/`):
- Image: `apptolast/invernaderos-api:latest`
- Replicas: 2-3 (high availability)
- Resources: Higher limits
- Profile: `prod`

**Development** (`../11-api-dev/`):
- Image: `apptolast/invernaderos-api:develop`
- Replicas: 1
- Resources: Lower limits
- Profile: `dev`
- Debug enabled

### Useful kubectl Commands

```bash
# View all resources
kubectl get all -n apptolast-invernadero-api

# Check pod status
kubectl get pods -n apptolast-invernadero-api

# View logs (API)
kubectl logs -f deployment/invernaderos-api-prod -n apptolast-invernadero-api

# View logs (TimescaleDB)
kubectl logs -f statefulset/timescaledb -n apptolast-invernadero-api

# Execute SQL in TimescaleDB
kubectl exec -it timescaledb-0 -n apptolast-invernadero-api -- psql -U admin -d postgres

# Execute SQL in PostgreSQL Metadata
kubectl exec -it postgresql-metadata-0 -n apptolast-invernadero-api -- psql -U admin -d postgres

# Port forward for local access
kubectl port-forward svc/invernaderos-api-prod 8080:8080 -n apptolast-invernadero-api

# Scale API replicas
kubectl scale deployment invernaderos-api-prod --replicas=3 -n apptolast-invernadero-api

# Restart API deployment
kubectl rollout restart deployment/invernaderos-api-prod -n apptolast-invernadero-api

# Check deployment status
kubectl rollout status deployment/invernaderos-api-prod -n apptolast-invernadero-api
```

### Docker Compose (Local Development)

For local development without Kubernetes:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f api

# Stop all services
docker-compose down

# Rebuild API
docker-compose up -d --build api
```

**Services included**:
- TimescaleDB (port 5432)
- PostgreSQL Metadata (port 5433)
- Redis (port 6379)
- EMQX MQTT (ports 1883, 18083)
- InvernaderosAPI (port 8080)

### Multi-Tenant Migration Notes

**IMPORTANT**: After deploying the multi-tenant code changes, you MUST:

1. **Run SQL migrations** V3-V10 on both dev and production databases
2. **Create tenant DEFAULT** using migration V7 for backward compatibility  
3. **Migrate existing data** to the DEFAULT tenant
4. **Update MQTT topics** to use `GREENHOUSE/{tenantId}` format for new clients
5. **Keep legacy `GREENHOUSE` topic** active during migration period

See `MIGRATION_GUIDE.md` for detailed step-by-step instructions.

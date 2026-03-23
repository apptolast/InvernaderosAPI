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
6. **Multi-Tenant Architecture**: UUID-based tenant isolation across all data stores (added Nov 2025)
7. **Staging Pipeline**: Bulk data validation and migration infrastructure for million-record operations

---

## Multi-Tenant Architecture

**Implementation Date**: November 2025 (Commits 8f37799, 446835f, dba5212)

### Overview

The system now supports full multi-tenant data isolation with UUID-based tenant identification across all databases and MQTT topics.

### MQTT Topic Structure

**Legacy Format** (backward compatible):
```
Topic: GREENHOUSE
Maps to: tenantId = "DEFAULT"
Use case: Existing systems during migration period
```

**Multi-Tenant Format**:
```
Topic Pattern: GREENHOUSE/{tenantId}

Examples:
- GREENHOUSE/SARA → tenantId = "SARA" (Vivero Sara greenhouse)
- GREENHOUSE/001 → tenantId = "001" (Generic tenant ID)
- GREENHOUSE/NARANJOS → tenantId = "NARANJOS" (Los Naranjos farm)
```

**Dynamic Topic Extraction** (GreenhouseDataListener.kt:39-43):
```kotlin
val tenantId = when {
    topic.startsWith("GREENHOUSE/") -> topic.substringAfter("GREENHOUSE/").takeWhile { it != '/' }
    topic == "GREENHOUSE" -> "DEFAULT"  // Legacy compatibility
    else -> "UNKNOWN"
}
```

### Database Schema Changes

**All tables now include tenant_id UUID field:**

**PostgreSQL Metadata** (schema: `metadata`):
- `tenants` - Master tenant registry (id UUID PK, company details, legal info, contact)
- `greenhouses` - Added tenant_id UUID FK, mqtt_topic, greenhouse_code
- `sensors` - Added tenant_id UUID FK, mqtt_topic, last_seen_at, is_active
- `actuators` - Added tenant_id UUID FK, mqtt_topic, control_mode
- `alerts` - Added tenant_id UUID FK, resolved_by_user_id UUID FK
- `users` - Linked to tenant_id for multi-tenant access control
- `mqtt_users` - Added tenant_id UUID FK for MQTT authentication routing

**TimescaleDB** (schema: `iot`):
- `sensor_readings` - **CRITICAL CHANGE** (Nov 16, 2025):
  - greenhouse_id: VARCHAR(50) → **UUID**
  - **NEW FIELD**: tenant_id UUID (indexed for multi-tenant queries)
  - Hypertable recreated with UUID support
  - Indexes: `idx_sensor_readings_tenant_time`, `idx_sensor_readings_greenhouse_sensor_time`

### Data Isolation

**Tenant Isolation Strategy**:
1. **MQTT Level**: Topic-based routing (`GREENHOUSE/{tenantId}`)
2. **Application Level**: GreenhouseDataListener extracts tenantId from topic path
3. **Database Level**: All queries filtered by tenant_id UUID
4. **Cache Level**: Redis keys include tenant context (future enhancement)

**Seed Data** (V7 migration + seed_data_realistic.sql):
- **DEFAULT tenant**: For backward compatibility with legacy GREENHOUSE topic
- **SARA tenant** (Vivero Sara): Spanish agricultural company, 3 greenhouses
- **NARANJOS tenant** (Agrícola Los Naranjos): Citrus farm, 2 greenhouses
- **SUR tenant** (Invernaderos del Sur): Southern Spain greenhouse operation

### Migration Considerations

**⚠️ IMPORTANT**:
- Existing sensor_readings data has **NULL tenant_id** (needs manual population)
- DEFAULT tenant UUID must match across PostgreSQL and TimescaleDB
- See `MIGRATION_GUIDE.md` (450+ lines) for step-by-step migration instructions
- Legacy `GREENHOUSE` topic remains active during migration period

### MQTT Echo Feature

**Purpose**: Bidirectional verification of data reception

**Implementation**:
```
MQTT Message → GreenhouseDataListener
    ↓
MqttMessageProcessor (process + cache + DB)
    ↓
ApplicationEventPublisher (broadcast to WebSocket)
    ↓
MqttPublishService.publishToResponseTopic() → GREENHOUSE/RESPONSE
```

**Use Case**: Allows hardware engineer (Jesús) to subscribe to `GREENHOUSE/RESPONSE` topic and verify API received sensor data correctly.

---

## Staging Infrastructure for Bulk Operations

**Implementation Date**: November 16, 2025 (Commit a4415c5)
**Migration**: V11__create_staging_infrastructure_timescaledb.sql (581 lines)

### Overview

Production-grade infrastructure for safely importing and validating million-record datasets with full audit trail.

### Staging Schema Tables

**1. staging.sensor_readings_raw**
```sql
Purpose: Receive unvalidated data from bulk imports
Constraints: Minimal (accepts malformed data for analysis)
Fields:
  - All fields from iot.sensor_readings (time, sensor_id, greenhouse_id, etc.)
  - batch_id UUID (groups related imports)
  - import_timestamp TIMESTAMPTZ (when record entered staging)
  - validation_status VARCHAR(20) DEFAULT 'pending' (pending, valid, invalid, migrated)
  - validation_errors TEXT[] (array of error messages)
  - source_system VARCHAR(100) (origin of data: mqtt, api, csv, etc.)

Indexes:
  - idx_staging_raw_batch_id (batch_id)
  - idx_staging_raw_validation_status (validation_status)
  - idx_staging_raw_import_timestamp (import_timestamp DESC)
```

**2. staging.sensor_readings_validated**
```sql
Purpose: Validated data ready for production migration
Constraints: Strict (enforces UUID types, foreign keys)
Fields: Same as iot.sensor_readings + batch_id, validated_at
Foreign Keys:
  - raw_reading_id → staging.sensor_readings_raw(id)
  - Enforces UUID types for greenhouse_id, tenant_id
```

**3. staging.bulk_import_log**
```sql
Purpose: Audit trail for all bulk operations
Fields:
  - batch_id UUID PK
  - operation_type VARCHAR(50) (import, validation, migration, rollback)
  - started_at, completed_at TIMESTAMPTZ
  - status VARCHAR(20) (running, completed, failed, partially_completed)
  - total_records INT
  - successful_records INT
  - failed_records INT
  - error_summary TEXT
  - executed_by VARCHAR(100) (user/system that triggered operation)
  - duration_seconds NUMERIC(10,2)

Indexes: idx_bulk_import_log_started_at, idx_bulk_import_log_status
```

**4. staging.validation_rules**
```sql
Purpose: Configurable validation rules per sensor type
Default Rules:
  - TEMPERATURE: -50 to 100°C
  - HUMIDITY: 0 to 100%
  - SOIL_MOISTURE: 0 to 100%
  - LIGHT_INTENSITY: 0 to 200000 lux
  - CO2_LEVEL: 0 to 5000 ppm
  - ATMOSPHERIC_PRESSURE: 800 to 1100 hPa

Fields:
  - sensor_type VARCHAR(50)
  - min_value, max_value NUMERIC
  - is_active BOOLEAN
  - description TEXT
```

### Stored Procedures

**1. staging.proc_validate_sensor_readings(p_batch_id UUID)**
```sql
Purpose: Validate raw data against rules, move to validated table
Logic:
  1. Fetch all pending records for batch_id
  2. For each record:
     - Check greenhouse_id is valid UUID
     - Check tenant_id is valid UUID
     - Check value is within sensor type range (from validation_rules)
     - Check required fields are not NULL
  3. If valid: INSERT into sensor_readings_validated
  4. If invalid: UPDATE validation_status='invalid', populate validation_errors
  5. Update bulk_import_log with statistics

Returns: JSON summary {total, valid, invalid, errors}
```

**2. staging.proc_migrate_staging_to_production(p_batch_id UUID, p_delete_after BOOLEAN)**
```sql
Purpose: Bulk INSERT from staging.sensor_readings_validated to iot.sensor_readings
Logic:
  1. Start transaction
  2. INSERT INTO iot.sensor_readings SELECT * FROM staging.sensor_readings_validated WHERE batch_id = p_batch_id
  3. UPDATE staging.sensor_readings_validated SET validation_status='migrated'
  4. If p_delete_after = TRUE: DELETE from staging tables
  5. Log success/failure to bulk_import_log
  6. Commit transaction

Performance: Uses batch INSERT (single query for all records)
Safety: Transactional (all-or-nothing)
```

**3. staging.proc_cleanup_staging(p_days_to_keep INT)**
```sql
Purpose: Cleanup old staging data
Logic:
  1. DELETE FROM staging.sensor_readings_raw WHERE import_timestamp < NOW() - p_days_to_keep
  2. DELETE FROM staging.sensor_readings_validated WHERE validated_at < NOW() - p_days_to_keep
  3. Keep bulk_import_log forever (audit requirement)

Recommended Schedule: Weekly CronJob, keep 30 days
```

### Continuous Aggregates

**iot.sensor_readings_hourly** (TimescaleDB materialized view):
```sql
Purpose: Pre-aggregated hourly statistics
Refresh: Every 1 hour
Data:
  - bucket (hour timestamp)
  - sensor_id, greenhouse_id, tenant_id
  - avg_value, min_value, max_value, stddev_value
  - percentile_50 (median), percentile_95
  - count (number of readings in hour)

Query Optimization: 60x faster than raw data queries for hourly dashboards
```

**iot.sensor_readings_daily_by_tenant** (TimescaleDB materialized view):
```sql
Purpose: Daily tenant-level aggregations
Refresh: Every 6 hours
Data:
  - day (date), tenant_id
  - total_readings, unique_sensors, unique_greenhouses
  - avg_temperature, avg_humidity, etc. (per sensor type)

Use Case: Tenant-level reporting, billing, SLA monitoring
```

### Usage Workflow

**Typical Bulk Import Flow**:
```sql
-- 1. Generate batch ID
SELECT gen_random_uuid() AS batch_id;  -- Returns: '550e8400-e29b-41d4-a716-446655440000'

-- 2. INSERT raw data (from CSV, API, etc.)
INSERT INTO staging.sensor_readings_raw (batch_id, time, sensor_id, greenhouse_id, tenant_id, value, unit, source_system)
VALUES
  ('550e8400-e29b-41d4-a716-446655440000', '2025-11-16 10:00:00', 'TEMP_01', 'valid-uuid', 'valid-tenant-uuid', 25.5, '°C', 'csv_import'),
  -- ... 1 million more records ...

-- 3. Validate data
SELECT * FROM staging.proc_validate_sensor_readings('550e8400-e29b-41d4-a716-446655440000');
-- Returns: {"total": 1000000, "valid": 998500, "invalid": 1500, "errors": ["Invalid UUID", "Value out of range"]}

-- 4. Review validation errors
SELECT sensor_id, validation_errors
FROM staging.sensor_readings_raw
WHERE batch_id = '550e8400-e29b-41d4-a716-446655440000'
  AND validation_status = 'invalid';

-- 5. Migrate valid data to production
SELECT * FROM staging.proc_migrate_staging_to_production('550e8400-e29b-41d4-a716-446655440000', FALSE);
-- Returns: {"migrated": 998500, "status": "completed", "duration_seconds": 45.2}

-- 6. Verify production data
SELECT COUNT(*) FROM iot.sensor_readings WHERE time >= '2025-11-16 10:00:00';

-- 7. Review audit log
SELECT * FROM staging.bulk_import_log WHERE batch_id = '550e8400-e29b-41d4-a716-446655440000';
```

### Performance Characteristics

- **Validation Speed**: ~20,000 records/second (on mid-range server)
- **Migration Speed**: ~50,000 records/second (batch INSERT)
- **Storage**: Staging tables use TimescaleDB compression (7-day policy)
- **Concurrency**: Multiple batches can be processed in parallel (different batch_ids)

### Safety Features

1. **Transactional**: All operations are atomic (rollback on failure)
2. **Audit Trail**: Every operation logged in bulk_import_log
3. **Validation**: Configurable rules prevent invalid data in production
4. **Isolation**: Staging data isolated from production queries
5. **Cleanup**: Automated cleanup prevents staging table bloat

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

**Implementation** (GreenhouseCacheService.kt lines 1-201)

**Primary Data Structure**: Sorted Set (ZSET)
```kotlin
Key: "greenhouse:messages"
Data Structure: Sorted Set (ZSET)
Score: timestamp.toEpochMilli() (Double - milliseconds since epoch)
Value: JSON serialized RealDataDto (22 fields)
Max Size: 1000 messages (auto-trimmed via ZREMRANGEBYRANK 0 -1001)
TTL: 24 hours (86400 seconds, renewed on each write)
```

**Operations** (with time complexity):
- `cacheMessage(RealDataDto)` - ZADD + ZREMRANGEBYRANK + EXPIRE - **O(log N)**
- `getRecentMessages(limit)` - ZREVRANGE 0 (limit-1) - **O(log N + M)** where M = result size
- `getMessagesByTimeRange(start, end)` - ZREVRANGEBYSCORE - **O(log N + M)**
- `getLatestMessage()` - ZREVRANGE 0 0 - **O(log N)**
- `countMessages()` - ZCARD - **O(1)**
- `clearCache()` - DEL - **O(1)**
- `getCacheStats()` - ZCARD + TTL + ZRANGE - **O(log N)**

**Performance Characteristics**:
- Memory-efficient: Compressed JSON strings
- Fast queries: O(log N) time complexity for most operations
- Automatic eviction: Oldest messages removed when > 1000
- Self-renewing TTL: 24-hour expiration refreshed on each write

---

## Redis Configuration (DEV & PROD)

### DEV Environment (Docker Compose)

**File**: `docker-compose.yaml` (lines 60-72)

**Container Configuration**:
```yaml
redis:
  image: redis:7-alpine
  container_name: invernaderos-redis
  command: redis-server --requirepass ${REDIS_PASSWORD}
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
  healthcheck:
    test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
  restart: unless-stopped
```

**Connection Details**:
- **Host**: `redis` (Docker internal) / `localhost` (external)
- **Port**: 6379
- **Password**: Environment variable `REDIS_PASSWORD`
- **Database**: 0 (default)
- **Volume**: `invernaderos-redis-data` (persistent storage)
- **Health Check**: Redis CLI ping every 10 seconds

**Local Access**:
```bash
# Connect to Redis CLI
docker exec -it invernaderos-redis redis-cli -a "${REDIS_PASSWORD}"

# Monitor real-time commands
docker exec -it invernaderos-redis redis-cli -a "${REDIS_PASSWORD}" MONITOR

# View sorted set contents
docker exec -it invernaderos-redis redis-cli -a "${REDIS_PASSWORD}" ZREVRANGE greenhouse:messages 0 10 WITHSCORES
```

---

### PROD Environment (Kubernetes)

**StatefulSet**: `../06-redis/statefulset.yaml`

**Pod Configuration**:
```yaml
image: redis:7-alpine
command: ["redis-server", "/etc/redis/redis.conf"]

resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 500m
    memory: 1Gi

volumeMounts:
  - name: data
    mountPath: /data
    subPath: redis
  - name: config
    mountPath: /etc/redis

securityContext:
  runAsUser: 1000
  runAsGroup: 1000
  fsGroup: 1000
```

**Redis Configuration** (`../02-configmaps/redis-config.yaml`):
```conf
# Memory Management
maxmemory 900mb
maxmemory-policy volatile-lru  # Evict keys with TTL using LRU algorithm

# Persistence
save 300 10                    # Save every 5 min if 10+ changes
save 60 10000                  # Save every 1 min if 10000+ changes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir /data

# Performance
timeout 300                    # Close idle clients after 5 minutes
tcp-keepalive 60
maxclients 10000

# Security
requirepass ${REDIS_PASSWORD}  # From Secret: redis-credentials
protected-mode yes
rename-command FLUSHDB ""      # Disabled for safety
rename-command FLUSHALL ""     # Disabled for safety
rename-command CONFIG ""       # Disabled for safety

# Logging
loglevel notice
logfile ""
```

**Service Configuration** (`../06-redis/service.yaml`):
```yaml
# Internal ClusterIP (for API pods)
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: apptolast-invernadero-api
spec:
  type: ClusterIP
  selector:
    app: redis
  ports:
    - name: redis
      port: 6379
      targetPort: 6379

---
# External NodePort (for debugging/monitoring)
apiVersion: v1
kind: Service
metadata:
  name: redis-nodeport
  namespace: apptolast-invernadero-api
spec:
  type: NodePort
  selector:
    app: redis
  ports:
    - name: redis
      port: 6379
      targetPort: 6379
      nodePort: 30379  # External access at node-ip:30379
```

**Access Details**:
- **Internal (from API pods)**: `redis.apptolast-invernadero-api.svc.cluster.local:6379`
- **External (for debugging)**: `<node-ip>:30379`
- **Password**: From Kubernetes Secret `redis-credentials`
- **Persistent Volume**: `/mnt/k8s-storage/invernaderos/redis` (host path, chown 1000:1000)

**kubectl Commands**:
```bash
# Connect to Redis CLI
kubectl exec -it $(kubectl get pods -n apptolast-invernadero-api -l app=redis -o name) -n apptolast-invernadero-api -- redis-cli -a "${REDIS_PASSWORD}"

# Monitor Redis performance
kubectl exec -it $(kubectl get pods -n apptolast-invernadero-api -l app=redis -o name) -n apptolast-invernadero-api -- redis-cli -a "${REDIS_PASSWORD}" --stat

# Get cache size
kubectl exec -it $(kubectl get pods -n apptolast-invernadero-api -l app=redis -o name) -n apptolast-invernadero-api -- redis-cli -a "${REDIS_PASSWORD}" ZCARD greenhouse:messages

# View logs
kubectl logs -f -n apptolast-invernadero-api -l app=redis
```

---

### Application Redis Configuration

**File**: `src/main/resources/application.yaml` (lines 74-101)

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:138.199.157.58}      # K8s node IP (default)
      port: ${REDIS_PORT:30379}                # NodePort (DEV: 6379)
      password: ${REDIS_PASSWORD:AppToLast2023%}
      database: 0
      timeout: 60000ms                         # 60 seconds
      connect-timeout: 10000ms                 # 10 seconds
      client-type: lettuce                     # Lettuce client (async, reactive)

      lettuce:
        pool:
          max-active: 100                      # Max connections
          max-idle: 50                         # Max idle connections
          min-idle: 10                         # Min idle connections
          max-wait: 3000ms                     # Max wait for connection
        shutdown-timeout: 2000ms

  cache:
    type: redis
    redis:
      time-to-live: 600000                     # 10 minutes (600000 ms)
      cache-null-values: false
      key-prefix: "ts-app::"                  # Prefix for @Cacheable keys
      use-key-prefix: true
```

**Connection Pooling**:
- **Max Active**: 100 concurrent connections
- **Max Idle**: 50 idle connections kept open
- **Min Idle**: 10 idle connections pre-created
- **Max Wait**: 3 seconds timeout for acquiring connection from pool

**Lettuce Client Benefits**:
- **Asynchronous**: Non-blocking I/O with Netty
- **Reactive**: Supports Spring WebFlux reactive streams
- **Thread-safe**: Single connection shared across threads
- **Auto-reconnect**: Handles Redis restarts gracefully

---

### Redis Monitoring & Maintenance

**Health Check Endpoint**:
```bash
# Check Redis connectivity
curl http://localhost:8080/api/greenhouse/cache/info

# Response:
{
  "totalMessages": 1000,
  "ttlSeconds": 86400,
  "maxCapacity": 1000,
  "utilizationPercentage": 100.0,
  "cacheType": "Redis Sorted Set",
  "oldestMessageTimestamp": "2025-11-15T10:30:00Z",
  "newestMessageTimestamp": "2025-11-16T10:30:00Z"
}
```

**Memory Usage Monitoring**:
```bash
# Get Redis memory stats
redis-cli INFO memory

# Key metrics:
# - used_memory_human: Actual memory used
# - used_memory_peak_human: Peak memory usage
# - mem_fragmentation_ratio: Should be < 1.5
# - evicted_keys: Number of keys evicted (should be 0 with volatile-lru)
```

**Performance Tuning**:
- **Eviction Policy**: `volatile-lru` evicts least recently used keys with TTL (perfect for time-series cache)
- **Persistence**: Configured for durability (save every 5 min if 10+ changes)
- **Compression**: RDB compression enabled (reduces disk usage)
- **Connection Pooling**: Tuned for high concurrency (100 max connections)

**⚠️ Security Warning**:
- **Issue**: Redis password currently in ConfigMap (`../02-configmaps/redis-config.yaml` line 19)
- **Risk**: ConfigMaps are not encrypted at rest
- **Recommendation**: Move password to Kubernetes Secret
- **Fix**: Use `valueFrom.secretKeyRef` instead of hardcoded value

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

### Runtime & Languages
- **Java**: 21 LTS (Oracle/OpenJDK)
  - Virtual Threads (Project Loom) available
  - Pattern Matching for switch
  - Record patterns
  - Sequenced Collections
- **Kotlin**: 2.2.21 ⚠️ **Upgraded from 1.9.25** (Commits 9bc4e5d, 8f4e9c0, 55b9dac - Nov 2025)
  - **K2 Compiler**: Much faster compilation times
  - **Context Receivers**: Advanced context management
  - **Data Objects**: Lightweight data classes
  - **Gradle Plugin**: org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21
  - **Spring Plugin**: org.jetbrains.kotlin.plugin.spring:2.2.21
  - **JPA Plugin**: org.jetbrains.kotlin.plugin.jpa:2.2.21

### Framework & Build
- **Spring Boot**: 3.5.7 (Spring Framework 6.x)
  - **Jakarta EE 10+**: Uses jakarta.* packages (NOT javax.*)
  - **SecurityFilterChain**: Modern security configuration (WebSecurityConfigurerAdapter deprecated)
  - **Native Compilation**: GraalVM support ready
  - **Actuator**: Metrics and health endpoints enabled
- **Gradle**: 8.14.3 with Kotlin DSL
  - Multi-project build
  - Dependency version management with BOM

### Messaging & Integration
- **MQTT**: Spring Integration MQTT 6.5.3 + Eclipse Paho 1.2.5
  - Inbound/Outbound adapters
  - Dynamic topic subscription
  - QoS levels: 0 (sensors), 1 (actuators), 2 (alerts)
- **WebSocket**: Spring WebSocket + STOMP
  - SockJS fallback support
  - Simple in-memory broker
  - Event-driven broadcasting

### Databases
- **TimescaleDB**: PostgreSQL 16 + TimescaleDB extension
  - Schema: `iot` (time-series data)
  - Hypertable with 7-day chunks
  - Compression after 7 days
  - 2-year retention policy
  - Continuous aggregates (hourly, daily)
- **PostgreSQL**: 16
  - Schema: `metadata` (reference data)
  - Multi-tenant support with UUID keys
  - JSONB fields for flexible data
  - PostGIS extension (geography data types)

### Caching
- **Redis**: 7-alpine
  - **Client**: Lettuce (async, reactive)
  - **Data Structure**: Sorted Set (ZSET) for time-series cache
  - **Eviction Policy**: volatile-lru (perfect for TTL keys)
  - **Persistence**: RDB snapshots (save 300 10, save 60 10000)
  - **Max Memory**: 900MB (K8s PROD), unlimited (Docker DEV)
  - **Connection Pool**: max-active=100, max-idle=50, min-idle=10

### Serialization & API
- **Jackson**: com.fasterxml.jackson.module:jackson-module-kotlin
  - @JsonProperty support for mixed key formats
  - Kotlin data class integration
  - ISO-8601 timestamp serialization
- **SpringDoc OpenAPI**: 2.8.14
  - Swagger UI at `/swagger-ui.html`
  - OpenAPI 3.0 JSON at `/v3/api-docs`
  - Try-it-out enabled for testing

### Containerization & Deployment
- **Docker**: Multi-stage builds
  - Base image: eclipse-temurin:21-jre-alpine
  - Optimized layers (dependencies cached separately)
  - Non-root user (appuser 1000:1000)
- **Kubernetes**: 1.28+
  - Namespace: apptolast-invernadero-api
  - StatefulSets (TimescaleDB, PostgreSQL, Redis)
  - Deployments (API, EMQX)
  - NodePort services for external access
  - PersistentVolumes on host paths

### Monitoring & Observability
- **Spring Boot Actuator**: Metrics and health endpoints
  - `/actuator/health` - Health check (UP/DOWN)
  - `/actuator/metrics` - Micrometer metrics
  - `/actuator/info` - Application info
  - `/actuator/prometheus` - Prometheus scraping endpoint (optional)
- **Logging**: Logback with SLF4J
  - Console logging (JSON format for K8s)
  - File logging (optional)
  - Log levels: DEBUG (dev), INFO (prod)

### CI/CD
- **GitHub Actions**: `.github/workflows/build-and-push.yml`
  - Triggers: Push to `main` or `develop` branches
  - Docker images: `apptolast/invernaderos-api:latest` (main), `apptolast/invernaderos-api:develop` (develop)
  - Registry: DockerHub
- **Dependabot**: Automated dependency updates (configured in `.github/dependabot.yml`)

### Development Tools
- **Flyway**: Database migration versioning
  - Location: `src/main/resources/db/migration`
  - Naming: V{version}__{description}.sql
  - Checksum validation (detects manual changes)
- **HikariCP**: JDBC connection pooling
  - TimescaleDB pool: max-pool-size=20, min-idle=5
  - PostgreSQL pool: max-pool-size=10, min-idle=2
- **JUnit 5 + MockK**: Testing framework (Kotlin-friendly mocking)

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

### Critical Issues (Can Break Production)

1. **⚠️ TimescaleDB Schema is 'iot' NOT 'public'** (CHANGED Nov 16, 2025 - Commit dba5212)
   - **Previous**: DEV used schema `public`, PROD used schema `iot` (inconsistent)
   - **Current**: BOTH DEV and PROD use schema `iot`
   - **Impact**: SensorReading.kt updated: `@Table(name = "sensor_readings", schema = "iot")` (line 32)
   - **Migration**: sensor_readings table was dropped and recreated with UUID types
   - **Data Loss**: ~4M test records in DEV were cleared, PROD started fresh
   - **Fix**: Always use `iot.sensor_readings` in queries, not `public.sensor_readings`

2. **⚠️ tenant_id is NULL for existing data**
   - **Issue**: All sensor_readings have `tenant_id = NULL` until manually populated
   - **Why**: Multi-tenant migration added UUID tenant_id field, but existing data not migrated
   - **Impact**: Queries filtering by tenant_id will exclude existing records
   - **Fix**: Run SQL script to populate tenant_id based on greenhouse_id → tenant association
   - **Script**: See `MIGRATION_GUIDE.md` section on "Populating tenant_id for existing data"

3. **⚠️ greenhouse_id changed from VARCHAR(50) to UUID** (Nov 16, 2025)
   - **Previous**: greenhouse_id was VARCHAR(50) (could be "001", "SARA", etc.)
   - **Current**: greenhouse_id is UUID (must be valid UUID like '550e8400-e29b-41d4-a716-446655440000')
   - **Impact**: Cannot INSERT sensor_readings with string IDs anymore
   - **Fix**: Use UUID.fromString() or ensure greenhouse table has UUID id column
   - **Example**: `SELECT id FROM metadata.greenhouses WHERE greenhouse_code = '001'`

4. **⚠️ Redis password in ConfigMap (Security Risk)**
   - **Issue**: Redis password hardcoded in `/home/admin/.../k8s/02-configmaps/redis-config.yaml` line 19
   - **Value**: `AppToLast2023%` (visible in ConfigMap)
   - **Risk**: ConfigMaps are NOT encrypted at rest in etcd
   - **Impact**: Anyone with kubectl access can read Redis password
   - **Fix**: Move to Kubernetes Secret with `valueFrom.secretKeyRef`
   - **Recommendation**: Use `redis-credentials` Secret (already exists for other components)

5. **⚠️ Simulation Mode Warning is Intentional**
   - **Log Message**: `"⚠️ SIMULATION MODE ENABLED - Generating fake greenhouse data"`
   - **When**: greenhouse.simulation.enabled = true in application.yaml
   - **Purpose**: Clearly indicates system is using simulated data, not real sensors
   - **Action**: This is NOT an error. If you see this in production, IMMEDIATELY disable simulation mode.
   - **Impact**: All sensor data is randomly generated, not from actual greenhouse hardware

6. **⚠️ MQTT Echo to GREENHOUSE/RESPONSE is Intentional**
   - **Behavior**: Every message received on `GREENHOUSE` is echoed to `GREENHOUSE/RESPONSE`
   - **Why**: Allows hardware engineer (Jesús) to verify API received data correctly
   - **Not a Bug**: This is bidirectional verification, not duplicate processing
   - **Performance**: Negligible impact (single MQTT publish per message received)

### Data Handling Issues

7. **Dual DataSource**: Always specify `@Qualifier` when injecting repositories or transaction managers
   - **Correct**: `@Qualifier("timescaleTransactionManager")`
   - **Wrong**: Autowiring without qualifier (Spring won't know which one)

8. **Data Transformation: 1 RealDataDto → 22 SensorReading entities**
   - **Input**: Single JSON payload with 22 numeric fields
   - **Output**: 22 separate database rows (one per field: temp, humidity, sectors, extractors)
   - **Why**: Normalized time-series data for efficient querying
   - **Don't expect**: 1-to-1 mapping between DTO and entity

9. **Batch Inserts for Performance**
   - **Use**: `repository.saveAll(list)` for bulk inserts
   - **Don't use**: Loop with `repository.save()` (N queries vs 1 query)
   - **Impact**: 22x faster (1 batch INSERT vs 22 individual INSERTs per message)

### MQTT & WebSocket Issues

10. **MQTT Topic Structure**
    - **Legacy**: `GREENHOUSE` → maps to tenantId = "DEFAULT"
    - **Multi-Tenant**: `GREENHOUSE/{tenantId}` → extracts tenantId from topic path
    - **Wrong Topics**: `greenhouse`, `GREENHOUSE/`, `SENSOR/001` (not recognized)

11. **WebSocket vs MQTT**
    - **Correct**: Mobile apps connect to WebSocket (STOMP), subscribe to `/topic/greenhouse/messages`
    - **Wrong**: Mobile apps trying to connect directly to MQTT broker (different protocol)
    - **Data Source**: WebSocket receives RealDataDto (22 fields), NOT individual SensorReading entities

### Data Format Issues

12. **DTO Format**: System uses `RealDataDto` (22 fields), NOT `GreenhouseMessageDto` (legacy)
    - **Current**: RealDataDto (TEMPERATURA, HUMEDAD, SECTORES, EXTRACTORES)
    - **Legacy**: GreenhouseMessageDto (sensor01, sensor02, setpoint01-03) - DEPRECATED

13. **JSON Mapping Inconsistency (INTENTIONAL)**
    - **Temperature/Humidity**: **SPACES** (`"TEMPERATURA INVERNADERO 01"`)
    - **Sectors/Extractors**: **UNDERSCORES** (`"INVERNADERO_01_SECTOR_01"`)
    - **Why**: Matches actual greenhouse hardware output format (Jesús's system)
    - **Don't "fix"**: Changing key format will break hardware integration

### Database & Caching Issues

14. **Redis Sorted Set Score**: Uses `timestamp.toEpochMilli()` as score
    - **Type**: Double (milliseconds since Unix epoch)
    - **Range Queries**: Use millisecond timestamps, not ISO strings
    - **Example**: `ZRANGEBYSCORE greenhouse:messages 1700000000000 1700100000000`

15. **TimescaleDB Hypertable Constraints**
    - **Primary Key**: (time, sensor_id) - BOTH fields required
    - **Cannot INSERT**: Records with duplicate (time, sensor_id) combinations
    - **Resolution**: Add microseconds to timestamp if multiple readings at same second

### Migration Files

16. **⚠️ Untracked Migration Files**
    - **Files Found**: V12, V13, V14 SQL files in project root (untracked by git)
    - **V12**: create_catalog_tables.sql / create_aggregation_tables.sql
    - **V13**: normalize_existing_tables.sql / create_continuous_aggregates.sql
    - **V14**: create_staging_tables.sql / optimize_sensor_readings.sql
    - **Action**: Review these files and determine if they should be executed
    - **Risk**: May conflict with existing V11 staging infrastructure

---

## Database Migration Summary

**Executed Migrations** (V2-V11 completed as of Nov 16, 2025):

### PostgreSQL Metadata Migrations

**V3: Add Tenant Company Fields** (14 fields added to tenants table)
```sql
ALTER TABLE metadata.tenants ADD COLUMN:
  - company_name VARCHAR(200)
  - legal_name VARCHAR(200)
  - tax_id VARCHAR(50)
  - address TEXT
  - city, country, postal_code VARCHAR
  - phone, email, website VARCHAR
  - industry VARCHAR(100)
  - is_active BOOLEAN DEFAULT true
```

**V4: Add Greenhouse MQTT Fields**
```sql
ALTER TABLE metadata.greenhouses ADD COLUMN:
  - tenant_id UUID REFERENCES tenants(id)
  - mqtt_topic VARCHAR(100)
  - greenhouse_code VARCHAR(50) UNIQUE
  - location_coordinates GEOGRAPHY(POINT, 4326)
  - timezone VARCHAR(50) DEFAULT 'Europe/Madrid'
```

**V5: Add Sensor Multi-Tenant Fields**
```sql
ALTER TABLE metadata.sensors ADD COLUMN:
  - tenant_id UUID REFERENCES tenants(id)
  - mqtt_topic VARCHAR(100)
  - last_seen_at TIMESTAMPTZ
  - is_active BOOLEAN DEFAULT true
  - metadata JSONB
```

**V6: Extend Actuators Table**
```sql
ALTER TABLE metadata.actuators:
  - CHANGE current_state from JSONB to VARCHAR(50)
  - ADD tenant_id UUID
  - ADD mqtt_topic VARCHAR(100)
  - ADD control_mode VARCHAR(20) CHECK (manual, automatic, scheduled)
```

**V7: Migrate Existing Data to DEFAULT Tenant**
```sql
-- Create DEFAULT tenant
INSERT INTO metadata.tenants (id, name, company_name, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', 'DEFAULT', 'Default Tenant', true);

-- Associate existing records with DEFAULT tenant
UPDATE metadata.greenhouses SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE metadata.sensors SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE metadata.actuators SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE metadata.mqtt_users SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
```

**V9: Add Multi-Tenant Indexes** (15+ indexes created)
```sql
CREATE INDEX idx_greenhouses_tenant_id ON metadata.greenhouses(tenant_id);
CREATE INDEX idx_sensors_tenant_id ON metadata.sensors(tenant_id);
CREATE INDEX idx_sensors_greenhouse_id ON metadata.sensors(greenhouse_id);
CREATE INDEX idx_sensors_tenant_greenhouse ON metadata.sensors(tenant_id, greenhouse_id);
CREATE INDEX idx_actuators_tenant_id ON metadata.actuators(tenant_id);
CREATE INDEX idx_actuators_greenhouse_id ON metadata.actuators(greenhouse_id);
CREATE INDEX idx_alerts_tenant_id ON metadata.alerts(tenant_id);
CREATE INDEX idx_alerts_unresolved ON metadata.alerts(tenant_id, is_resolved) WHERE is_resolved = false;
-- ... and more
```

**V10: Extend Alerts Table** (Multi-tenant alerts with severity levels)
```sql
ALTER TABLE metadata.alerts ADD COLUMN:
  - tenant_id UUID REFERENCES tenants(id)
  - resolved_by_user_id UUID REFERENCES users(id)
  - updated_at TIMESTAMPTZ DEFAULT NOW()

CREATE TYPE alert_severity AS ENUM ('INFO', 'WARNING', 'ERROR', 'CRITICAL', 'LOW', 'MEDIUM', 'HIGH');
CREATE TYPE alert_type AS ENUM ('THRESHOLD_EXCEEDED', 'SENSOR_OFFLINE', 'ACTUATOR_FAILURE', 'SYSTEM_ERROR');

ALTER TABLE metadata.alerts
  ADD COLUMN severity alert_severity DEFAULT 'INFO',
  ADD COLUMN alert_type alert_type,
  ADD COLUMN alert_data JSONB;
```

### TimescaleDB Migrations

**V2: Fix Composite Primary Key**
```sql
-- Fix sensor_readings primary key to include sensor_id
ALTER TABLE public.sensor_readings DROP CONSTRAINT sensor_readings_pkey;
ALTER TABLE public.sensor_readings ADD PRIMARY KEY (time, sensor_id);
```

**V8: UUID Migration + Schema Change to 'iot'** (CRITICAL - Nov 16, 2025)
```sql
-- Drop existing table (data loss: ~4M DEV records, 0 PROD records)
DROP TABLE IF EXISTS public.sensor_readings CASCADE;

-- Create in new 'iot' schema with UUID types
CREATE SCHEMA IF NOT EXISTS iot;

CREATE TABLE iot.sensor_readings (
    time TIMESTAMPTZ NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    greenhouse_id UUID NOT NULL,           -- CHANGED: VARCHAR(50) → UUID
    tenant_id UUID,                        -- NEW FIELD
    sensor_type VARCHAR(30) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(20),
    metadata JSONB,
    PRIMARY KEY (time, sensor_id)
);

-- Convert to hypertable
SELECT create_hypertable('iot.sensor_readings', 'time', chunk_time_interval => INTERVAL '7 days');

-- Configure compression (after 7 days)
ALTER TABLE iot.sensor_readings SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'sensor_id, greenhouse_id',
  timescaledb.compress_orderby = 'time DESC'
);

-- Configure retention (keep 2 years)
SELECT add_retention_policy('iot.sensor_readings', INTERVAL '2 years');

-- Add indexes
CREATE INDEX idx_sensor_readings_greenhouse_id ON iot.sensor_readings(greenhouse_id, time DESC);
CREATE INDEX idx_sensor_readings_tenant_time ON iot.sensor_readings(tenant_id, time DESC) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_sensor_readings_greenhouse_sensor_time ON iot.sensor_readings(greenhouse_id, sensor_id, time DESC);
```

**V11: Staging Infrastructure** (581 lines - see "Staging Infrastructure" section above)
```sql
-- Creates staging schema with 4 tables:
CREATE SCHEMA staging;
CREATE TABLE staging.sensor_readings_raw (...);
CREATE TABLE staging.sensor_readings_validated (...);
CREATE TABLE staging.bulk_import_log (...);
CREATE TABLE staging.validation_rules (...);

-- Creates 3 stored procedures:
CREATE FUNCTION staging.proc_validate_sensor_readings(UUID);
CREATE FUNCTION staging.proc_migrate_staging_to_production(UUID, BOOLEAN);
CREATE FUNCTION staging.proc_cleanup_staging(INT);

-- Creates 2 continuous aggregates:
CREATE MATERIALIZED VIEW iot.sensor_readings_hourly ...;
CREATE MATERIALIZED VIEW iot.sensor_readings_daily_by_tenant ...;
```

### Pending Migrations (Untracked Files - Require Review)

**V12 Options** (2 conflicting files found):
- `V12__create_catalog_tables.sql` - Unknown contents
- `V12__create_aggregation_tables.sql` - Unknown contents
- **Action**: Determine which V12 to use, or merge into single migration

**V13 Options** (2 conflicting files found):
- `V13__normalize_existing_tables.sql` - Unknown contents
- `V13__create_continuous_aggregates.sql` - May conflict with V11 continuous aggregates
- **Action**: Review for conflicts with existing V11 aggregates

**V14 Options** (2 conflicting files found):
- `V14__create_staging_tables.sql` - May conflict with V11 staging schema
- `V14__optimize_sensor_readings.sql` - Unknown contents
- **Action**: Review for conflicts with existing V11 staging infrastructure

### Migration Execution Order

**Correct Order** (already executed):
```
V2 → V3 → V4 → V5 → V6 → V7 → V8 → V9 → V10 → V11
```

**⚠️ DO NOT**:
- Skip migrations (Flyway will detect gaps and fail)
- Modify executed migration files (checksum mismatch)
- Run migrations manually (use Flyway for tracking)

### Post-Migration Actions Required

1. **Populate tenant_id for existing sensor_readings**:
   ```sql
   UPDATE iot.sensor_readings sr
   SET tenant_id = g.tenant_id
   FROM metadata.greenhouses g
   WHERE sr.greenhouse_id = g.id
     AND sr.tenant_id IS NULL;
   ```

2. **Verify DEFAULT tenant UUID matches**:
   ```sql
   -- PostgreSQL
   SELECT id FROM metadata.tenants WHERE name = 'DEFAULT';

   -- TimescaleDB (should match)
   SELECT DISTINCT tenant_id FROM iot.sensor_readings WHERE tenant_id IS NOT NULL LIMIT 1;
   ```

3. **Test multi-tenant MQTT topics**:
   ```bash
   # Publish to GREENHOUSE/SARA
   mosquitto_pub -t "GREENHOUSE/SARA" -m '{"TEMPERATURA INVERNADERO 01": 25.5}'

   # Verify tenant_id = 'SARA' in database
   SELECT tenant_id, COUNT(*) FROM iot.sensor_readings WHERE tenant_id IS NOT NULL GROUP BY tenant_id;
   ```

4. **Monitor continuous aggregates refresh**:
   ```sql
   -- Check last refresh time
   SELECT view_name, materialized_only, last_run_started_at
   FROM timescaledb_information.continuous_aggregates
   WHERE view_schema = 'iot';
   ```

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
- **Database**: `greenhouse_timeseries_dev` (DEV) / `greenhouse_timeseries_prod` (PROD)
- **Schema**: `iot` ⚠️ **CHANGED from 'public' on Nov 16, 2025**
- **Table**: `sensor_readings` (UUID greenhouse_id, UUID tenant_id)

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

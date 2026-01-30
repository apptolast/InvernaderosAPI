# FRONTEND MIGRATION GUIDE: UUID to BIGINT

**Fecha**: 2026-01-08
**Version**: 1.0
**Status**: COMPLETO - Listo para implementar en frontend

---

## RESUMEN EJECUTIVO

El backend ha migrado **TODOS** los identificadores de `UUID` (String de 36 caracteres) a `BIGINT` (Long/Int64).

### Cambio Principal

```kotlin
// ANTES (UUID)
val tenantId: String = "550e8400-e29b-41d4-a716-446655440000"
val greenhouseId: String = "123e4567-e89b-12d3-a456-426614174000"

// AHORA (Long)
val tenantId: Long = 1
val greenhouseId: Long = 5
```

### Impacto

| Aspecto | Cambio |
|---------|--------|
| **Tipo de dato** | `String` (UUID) → `Long` / `Int64` |
| **Longitud** | 36 caracteres → ~19 dígitos máximo |
| **JSON** | `"id": "uuid-string"` → `"id": 123` (número) |
| **URL paths** | `/api/v1/greenhouses/uuid-string` → `/api/v1/greenhouses/123` |

---

## TABLAS AFECTADAS

### PostgreSQL (Schema: `metadata`)

| Tabla | Columnas cambiadas a BIGINT |
|-------|---------------------------|
| `tenants` | `id` |
| `users` | `id`, `tenant_id` |
| `greenhouses` | `id`, `tenant_id` |
| `devices` | `id`, `tenant_id`, `greenhouse_id` |
| `sensors` | `id`, `greenhouse_id`, `tenant_id` |
| `actuators` | `id`, `greenhouse_id`, `tenant_id` |
| `alerts` | `id`, `greenhouse_id`, `tenant_id` |
| `sectors` | `id`, `greenhouse_id` |
| `settings` | `id`, `greenhouse_id`, `tenant_id` |
| `command_history` | `id`, `device_id`, `user_id` |

### TimescaleDB (Schema: `iot`)

| Tabla | Columnas cambiadas a BIGINT |
|-------|---------------------------|
| `readings` | `device_id` |
| `sensor_readings_daily` | `greenhouse_id`, `tenant_id` |
| `sensor_readings_hourly` | `greenhouse_id`, `tenant_id` |
| `sensor_readings_monthly` | `greenhouse_id`, `tenant_id` |
| `greenhouse_daily_summary` | `greenhouse_id`, `tenant_id` |
| `sensor_performance_daily` | `sensor_id`, `greenhouse_id`, `tenant_id` |

---

## CAMBIOS REQUERIDOS EN FRONTEND

### 1. Modelos de Datos (Data Classes / DTOs)

#### Kotlin Multiplatform

```kotlin
// ANTES
data class Tenant(
    val id: String,  // UUID como String
    val name: String,
    // ...
)

data class Greenhouse(
    val id: String,       // UUID
    val tenantId: String, // UUID
    val name: String,
    // ...
)

// AHORA
data class Tenant(
    val id: Long,     // BIGINT
    val name: String,
    // ...
)

data class Greenhouse(
    val id: Long,        // BIGINT
    val tenantId: Long,  // BIGINT
    val name: String,
    // ...
)
```

#### Swift/iOS

```swift
// ANTES
struct Tenant: Codable {
    let id: String  // UUID
    let name: String
}

// AHORA
struct Tenant: Codable {
    let id: Int64   // BIGINT
    let name: String
}
```

#### Dart/Flutter

```dart
// ANTES
class Tenant {
  final String id;  // UUID
  final String name;
}

// AHORA
class Tenant {
  final int id;     // BIGINT (int en Dart es int64)
  final String name;
}
```

### 2. Serialización JSON

#### Respuestas del API

```json
// ANTES - UUID como strings
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Mi Invernadero"
}

// AHORA - BIGINT como números
{
  "id": 1,
  "tenantId": 5,
  "name": "Mi Invernadero"
}
```

#### Kotlin Serialization

```kotlin
// ANTES
@Serializable
data class Greenhouse(
    val id: String,
    @SerialName("tenant_id") val tenantId: String
)

// AHORA
@Serializable
data class Greenhouse(
    val id: Long,
    @SerialName("tenant_id") val tenantId: Long
)
```

### 3. URLs y Paths de API

```kotlin
// ANTES
val url = "${baseUrl}/api/v1/greenhouses/550e8400-e29b-41d4-a716-446655440000"
val url = "${baseUrl}/api/v1/tenants/123e4567-e89b-12d3-a456-426614174000/greenhouses"

// AHORA
val url = "${baseUrl}/api/v1/greenhouses/1"
val url = "${baseUrl}/api/v1/tenants/5/greenhouses"
```

### 4. Parámetros de Query

```kotlin
// ANTES
fun getDevices(tenantId: String, greenhouseId: String)
GET /api/v1/devices?tenantId=uuid-string&greenhouseId=uuid-string

// AHORA
fun getDevices(tenantId: Long, greenhouseId: Long)
GET /api/v1/devices?tenantId=1&greenhouseId=5
```

### 5. Almacenamiento Local (SharedPreferences, UserDefaults, etc.)

```kotlin
// ANTES
val tenantId: String = preferences.getString("tenant_id", "")
val greenhouseId: String = preferences.getString("greenhouse_id", "")

// AHORA
val tenantId: Long = preferences.getLong("tenant_id", 0L)
val greenhouseId: Long = preferences.getLong("greenhouse_id", 0L)
```

### 6. Comparaciones y Validaciones

```kotlin
// ANTES
if (greenhouse.id == selectedGreenhouseId) { ... }
if (tenantId.isNotEmpty()) { ... }
if (greenhouse.id != null && greenhouse.id != "") { ... }

// AHORA
if (greenhouse.id == selectedGreenhouseId) { ... }  // Mismo, Long == Long
if (tenantId > 0) { ... }  // Validación de BIGINT
if (greenhouse.id != null && greenhouse.id > 0) { ... }
```

### 7. WebSocket/STOMP Messages

```kotlin
// ANTES - Los mensajes incluían UUIDs
data class GreenhouseMessage(
    val greenhouseId: String,  // UUID
    val deviceId: String,      // UUID
    val timestamp: Instant,
    val value: Double
)

// AHORA - Los mensajes incluyen Long
data class GreenhouseMessage(
    val greenhouseId: Long,    // BIGINT
    val deviceId: Long,        // BIGINT
    val timestamp: Instant,
    val value: Double
)
```

---

## LISTA DE ARCHIVOS A MODIFICAR

### Modelos/DTOs (Actualizar tipos)

```
src/commonMain/kotlin/models/
├── Tenant.kt           // id: String → Long
├── User.kt             // id, tenantId: String → Long
├── Greenhouse.kt       // id, tenantId: String → Long
├── Device.kt           // id, tenantId, greenhouseId: String → Long
├── Sensor.kt           // id, greenhouseId, tenantId: String → Long
├── Actuator.kt         // id, greenhouseId, tenantId: String → Long
├── Alert.kt            // id, greenhouseId, tenantId: String → Long
├── Sector.kt           // id, greenhouseId: String → Long
├── Setting.kt          // id, greenhouseId, tenantId: String → Long
├── CommandHistory.kt   // id, deviceId, userId: String → Long
└── Reading.kt          // deviceId: String → Long
```

### Repositorios/Data Sources

```
src/commonMain/kotlin/data/
├── TenantRepository.kt
├── GreenhouseRepository.kt
├── DeviceRepository.kt
├── SensorRepository.kt
├── AlertRepository.kt
└── ...
```

Actualizar:
- Firmas de métodos: `fun getById(id: String)` → `fun getById(id: Long)`
- URLs de API
- Parseo de respuestas

### ViewModels/Presenters

Actualizar las referencias a IDs en estados y acciones:

```kotlin
// ANTES
data class GreenhouseListState(
    val selectedGreenhouseId: String? = null,
    val greenhouses: List<Greenhouse> = emptyList()
)

// AHORA
data class GreenhouseListState(
    val selectedGreenhouseId: Long? = null,
    val greenhouses: List<Greenhouse> = emptyList()
)
```

### Navegación/Rutas

```kotlin
// ANTES
sealed class Route {
    data class GreenhouseDetail(val id: String) : Route()
}

// AHORA
sealed class Route {
    data class GreenhouseDetail(val id: Long) : Route()
}
```

---

## MIGRACIÓN DE DATOS LOCALES

Si el frontend almacena IDs localmente (cache, favoritos, etc.), se necesita migrar:

```kotlin
fun migrateLocalStorage() {
    val oldTenantId = preferences.getString("tenant_id", null)
    if (oldTenantId != null && oldTenantId.contains("-")) {
        // Es un UUID antiguo - obtener el nuevo ID del servidor
        val newTenantId = api.getTenantByUuid(oldTenantId).id
        preferences.putLong("tenant_id", newTenantId)
        preferences.remove("tenant_id_string")  // Limpiar antiguo si existe
    }
}
```

---

## ENDPOINTS AFECTADOS

Todos los endpoints que usan IDs ahora esperan/devuelven `Long`:

### Tenants
```
GET    /api/v1/tenants/{id}           // id: Long
POST   /api/v1/tenants                // body.id: Long (optional for create)
PUT    /api/v1/tenants/{id}           // id: Long
DELETE /api/v1/tenants/{id}           // id: Long
```

### Greenhouses
```
GET    /api/v1/greenhouses/{id}                    // id: Long
GET    /api/v1/tenants/{tenantId}/greenhouses      // tenantId: Long
POST   /api/v1/greenhouses                         // body.id, body.tenantId: Long
PUT    /api/v1/greenhouses/{id}                    // id: Long
DELETE /api/v1/greenhouses/{id}                    // id: Long
```

### Devices
```
GET    /api/v1/devices/{id}                        // id: Long
GET    /api/v1/greenhouses/{greenhouseId}/devices  // greenhouseId: Long
POST   /api/v1/devices                             // body includes Long IDs
```

### Sensors, Actuators, Alerts, etc.
(Mismo patrón - todos los IDs son ahora Long)

---

## COMPATIBILIDAD TEMPORAL (NO DISPONIBLE)

**IMPORTANTE**: NO hay compatibilidad hacia atrás. El frontend DEBE actualizarse antes de conectar al backend migrado.

El backend NO acepta UUIDs en ningún formato:
- ❌ Path: `/api/v1/greenhouses/550e8400-e29b-41d4-a716-446655440000`
- ❌ Query: `?tenantId=550e8400-e29b-41d4-a716-426655440000`
- ❌ Body: `{ "id": "550e8400-e29b-41d4-a716-446655440000" }`

Solo acepta Long:
- ✅ Path: `/api/v1/greenhouses/1`
- ✅ Query: `?tenantId=5`
- ✅ Body: `{ "id": 1 }`

---

## CHECKLIST DE MIGRACIÓN

### Fase 1: Preparación
- [ ] Revisar todos los modelos de datos
- [ ] Identificar todos los archivos que usan UUID/String para IDs
- [ ] Crear rama de migración

### Fase 2: Actualizar Modelos
- [ ] Cambiar `id: String` → `id: Long` en todos los DTOs
- [ ] Cambiar `tenantId: String` → `tenantId: Long`
- [ ] Cambiar `greenhouseId: String` → `greenhouseId: Long`
- [ ] Cambiar otros IDs relacionales (deviceId, sensorId, etc.)

### Fase 3: Actualizar Serialización
- [ ] Verificar que los parsers JSON manejan números (no strings)
- [ ] Actualizar @SerialName si es necesario
- [ ] Probar deserialización con respuestas de prueba

### Fase 4: Actualizar Capa de Red
- [ ] Actualizar construcción de URLs
- [ ] Actualizar parámetros de query
- [ ] Actualizar body de requests

### Fase 5: Actualizar UI/ViewModels
- [ ] Actualizar tipos en estados
- [ ] Actualizar comparaciones
- [ ] Actualizar navegación

### Fase 6: Actualizar Almacenamiento Local
- [ ] Migrar SharedPreferences/UserDefaults
- [ ] Actualizar cache local si existe
- [ ] Limpiar datos antiguos

### Fase 7: Testing
- [ ] Probar login/autenticación
- [ ] Probar listado de invernaderos
- [ ] Probar detalle de invernadero
- [ ] Probar sensores y dispositivos
- [ ] Probar alertas
- [ ] Probar WebSocket/tiempo real
- [ ] Probar casos edge (IDs grandes, etc.)

### Fase 8: Despliegue
- [ ] Coordinar despliegue con backend
- [ ] Publicar nueva versión de app
- [ ] Monitorear errores en producción

---

## EJEMPLOS COMPLETOS

### Ejemplo 1: Obtener Lista de Invernaderos

```kotlin
// ANTES
interface GreenhouseApi {
    @GET("api/v1/tenants/{tenantId}/greenhouses")
    suspend fun getGreenhouses(
        @Path("tenantId") tenantId: String
    ): List<GreenhouseResponse>
}

data class GreenhouseResponse(
    val id: String,
    val tenantId: String,
    val name: String
)

// AHORA
interface GreenhouseApi {
    @GET("api/v1/tenants/{tenantId}/greenhouses")
    suspend fun getGreenhouses(
        @Path("tenantId") tenantId: Long
    ): List<GreenhouseResponse>
}

data class GreenhouseResponse(
    val id: Long,
    val tenantId: Long,
    val name: String
)
```

### Ejemplo 2: Crear Alerta

```kotlin
// ANTES
data class CreateAlertRequest(
    val greenhouseId: String,
    val tenantId: String,
    val message: String,
    val severity: String
)

// AHORA
data class CreateAlertRequest(
    val greenhouseId: Long,
    val tenantId: Long,
    val message: String,
    val severity: String
)
```

### Ejemplo 3: WebSocket Handler

```kotlin
// ANTES
fun onMessage(json: String) {
    val message = Json.decodeFromString<WebSocketMessage>(json)
    val greenhouseId: String = message.greenhouseId  // UUID string
    updateGreenhouse(greenhouseId)
}

// AHORA
fun onMessage(json: String) {
    val message = Json.decodeFromString<WebSocketMessage>(json)
    val greenhouseId: Long = message.greenhouseId  // BIGINT number
    updateGreenhouse(greenhouseId)
}
```

---

## PREGUNTAS FRECUENTES

### ¿Por qué este cambio?

1. **Rendimiento**: BIGINT es ~50% más pequeño que UUID (8 bytes vs 16 bytes)
2. **Compresión**: TimescaleDB comprime BIGINT mucho mejor que UUID
3. **Velocidad de índices**: ~40% más rápido en búsquedas
4. **Simplicidad**: Los números son más fáciles de debuggear y recordar

### ¿Los IDs existentes cambiaron?

Sí. Los datos se migraron con nuevos IDs numéricos. No hay correlación directa entre el UUID anterior y el nuevo BIGINT.

### ¿Cómo obtengo los nuevos IDs?

El frontend debe obtener los IDs del servidor. No intentes convertir UUIDs locales a Long - simplemente haz un fresh login y obtén los datos actualizados del API.

### ¿Qué pasa con el cache local?

Debe ser invalidado/limpiado. Los IDs locales antiguos (UUID strings) ya no son válidos.

### ¿Los endpoints cambiaron de estructura?

No. Solo cambiaron los tipos de datos de los IDs. Las rutas, verbos HTTP y estructura de respuesta son los mismos.

---

## SOPORTE

Si tienes preguntas sobre la migración, contacta al equipo de backend antes de implementar cambios.

**Última actualización**: 2026-01-08

# Prompt técnico para Claude Code en Android Studio (`GreenhouseFronts`)

**Pásale a tu Claude Code en Android Studio TODO este documento (el bloque entre `=== INICIO PROMPT ===` y `=== FIN PROMPT ===`). Es autosuficiente.**

---

```text
=== INICIO PROMPT PARA CLAUDE CODE EN apptolast/GreenhouseFronts ===

# Conectar la pantalla "Histórico de alertas" al nuevo endpoint
# /api/v1/alerts/history/tenant/{tenantId} del backend

## Contexto

El backend `apptolast/InvernaderosAPI` acaba de añadir un endpoint nuevo, ya
desplegado en dev (https://inverapi-dev.apptolast.com) y prod
(https://inverapi-prod.apptolast.com), pensado específicamente para la pantalla
"Histórico" de esta app móvil:

    GET /api/v1/alerts/history/tenant/{tenantId}?limit=100

Devuelve **todas** las alertas que han existido para ese tenant (activas y
resueltas), ordenadas por `createdAt DESC`. El bug que arregla: la pantalla
"Histórico" llamaba a `GET /api/v1/alerts?tenantId=X&isResolved=true&limit=100`
y por construcción solo devolvía las alertas que ya se hubieran resuelto. Como
en producción real apenas se ha resuelto ninguna alerta todavía, el usuario veía
`[]` y la pantalla salía vacía a pesar de tener alertas activas. Decisión de
producto: "Histórico" debe mostrar el feed completo (activas + resueltas).

La URL de la pestaña **"Activas"** sigue siendo correcta y NO se toca:

    GET /api/v1/alerts?tenantId={tenantId}&isResolved=false&limit=100

(Si el cliente actualmente usa `/api/v1/alerts/unresolved/tenant/{tenantId}` para
contar las alertas activas en las cards de invernadero — visible en
`GreenhouseApiService.getUnresolvedAlerts` — eso TAMPOCO se toca.)

## Contrato del endpoint nuevo (verificado contra dev y prod)

**URL**: `${baseUrl}/alerts/history/tenant/{tenantId}` donde `baseUrl` es
`Environment.current.baseUrl` + `/api/v1` (igual que el resto de servicios del
repo — ver `util/Environment.kt` y `data/remote/KtorClient.kt`).

**Método**: `GET`
**Auth**: Bearer JWT (el `AUTHENTICATED_CLIENT` de Koin ya lo inyecta).
**Path param**: `tenantId: Long`
**Query param**: `limit: Int` (opcional, default `100` en backend; recomendado
mandarlo desde cliente para ser explícito).
**Response 200**: `application/json`, array de objetos `AlertResponse` (forma
literal traída del logcat real contra dev):

```json
[
  {
    "id": 823997985639380207,
    "code": "ALT-00010",
    "tenantId": 814997898604790412,
    "sectorId": 820685609276048132,
    "sectorCode": "SEC-00033",
    "alertTypeId": 3,
    "alertTypeName": "ACTUATOR FAILURE",
    "severityId": 4,
    "severityName": "CRITICAL",
    "severityLevel": 4,
    "message": "Vaya movida de calor",
    "description": null,
    "clientName": "Temperatura muy alta",
    "isResolved": false,
    "resolvedAt": null,
    "resolvedByUserId": null,
    "resolvedByUserName": null,
    "createdAt": "2026-03-23T19:13:48.628792Z",
    "updatedAt": "2026-04-30T23:52:38.919178Z"
  }
]
```

**Tipos exactos del backend** (verificados contra `AlertResponse.kt` del repo
del backend):

| Campo                | Tipo            | Notas                                              |
| -------------------- | --------------- | -------------------------------------------------- |
| `id`                 | `Long`          | Siempre presente.                                  |
| `code`               | `String`        | Ej. `"ALT-00010"`. Siempre presente.               |
| `tenantId`           | `Long`          | Siempre presente.                                  |
| `sectorId`           | `Long`          | Siempre presente.                                  |
| `sectorCode`         | `String?`       | Puede venir `null` si el sector no tiene código.   |
| `alertTypeId`        | `Short?`        | Nullable.                                          |
| `alertTypeName`      | `String?`       | Nullable.                                          |
| `severityId`         | `Short?`        | Nullable.                                          |
| `severityName`       | `String?`       | `"INFO" | "WARNING" | "ERROR" | "CRITICAL"` o null.|
| `severityLevel`      | `Short?`        | 1..4 o null.                                       |
| `message`            | `String?`       | Nullable.                                          |
| `description`        | `String?`       | Nullable.                                          |
| `clientName`         | `String?`       | Nombre legible para el usuario final.              |
| `isResolved`         | `Boolean`       | Siempre presente.                                  |
| `resolvedAt`         | `String?`       | ISO-8601 UTC, null si no resuelta.                 |
| `resolvedByUserId`   | `Long?`         | Nullable.                                          |
| `resolvedByUserName` | `String?`       | Nullable.                                          |
| `createdAt`          | `String`        | ISO-8601 UTC. Siempre presente.                    |
| `updatedAt`          | `String`        | ISO-8601 UTC. Siempre presente.                    |

> **Sobre `Short` en KMP**: el backend serializa estos como números pequeños.
> En el cliente puedes usar `Short?` o `Int?` indistintamente — kotlinx
> serialization los acepta y la app ya tiene `coerceInputValues = true`,
> `ignoreUnknownKeys = true` y `isLenient = true` configurados en
> `di/DataModule.kt`. Recomendado: `Int?` para evitar tener que castear en UI.

## Acción concreta que tienes que ejecutar

### Paso 0 — Detecta el estado actual de tu rama

Estás en `feature/alert-notifications` (rama remota). El usuario probablemente
tiene **commits locales encima** con la pantalla de Histórico medio hecha
(donde está el bug de `?isResolved=true`). Antes de tocar nada:

1. `git status` — para ver el árbol de trabajo.
2. `git log feature/alert-notifications..HEAD --oneline` — para ver los commits
   locales.
3. **Lista todos los archivos `*Alert*` del repo** y enséñame su ruta. Esto
   incluye al menos:
   - `composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/data/model/greenhouse/GreenhouseDtos.kt`
     (ya tiene un `AlertResponse` recortado con solo `id`, `sectorId`,
     `clientName`, `isResolved` — usado para contar alertas en las cards).
   - Cualquier `AlertApiService`, `AlertRepository`, `AlertHistoryViewModel`,
     `AlertsScreen` o `AlertHistoryScreen` que el usuario haya creado
     localmente.

Sin esto **no toques código**. Pídeme la lista y espera.

### Paso 1 — Modelo de datos

Revisa el `AlertResponse` actual en
`composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/data/model/greenhouse/GreenhouseDtos.kt`:

```kotlin
@Serializable
data class AlertResponse(
    val id: Long,
    val sectorId: Long,
    val clientName: String? = null,
    val isResolved: Boolean = false,
)
```

Este DTO está pensado solo para contar/agrupar (lo usa
`GreenhouseRepositoryImpl` para los counters de las cards). **NO lo modifiques
en el sitio actual** — romperías la suposición de los counters. En su lugar:

**Opción A (recomendada y mínima)**. Crea un DTO nuevo dedicado al detalle:

```kotlin
// composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/data/model/alert/AlertDetailResponse.kt
package com.apptolast.greenhousefronts.data.model.alert

import kotlinx.serialization.Serializable

/**
 * Response for endpoints that return the full alert payload:
 *  - GET /api/v1/alerts/history/tenant/{tenantId}
 *  - GET /api/v1/alerts/{id}
 *  - GET /api/v1/alerts/unresolved/tenant/{tenantId} (legacy, returns the same shape)
 *
 * Mirrors AlertResponse on the backend. Field order matches the Kotlin record
 * exposed by AlertController. JSON config (`ignoreUnknownKeys = true`) tolerates
 * future field additions on the server.
 */
@Serializable
data class AlertDetailResponse(
    val id: Long,
    val code: String,
    val tenantId: Long,
    val sectorId: Long,
    val sectorCode: String? = null,
    val alertTypeId: Int? = null,
    val alertTypeName: String? = null,
    val severityId: Int? = null,
    val severityName: String? = null,
    val severityLevel: Int? = null,
    val message: String? = null,
    val description: String? = null,
    val clientName: String? = null,
    val isResolved: Boolean = false,
    val resolvedAt: String? = null,
    val resolvedByUserId: Long? = null,
    val resolvedByUserName: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
```

> Si el usuario ya tiene un DTO equivalente creado localmente con otro nombre,
> **úsalo en su lugar** y reporta cuál has detectado. No dupliques.

### Paso 2 — API service

El repo ya tiene un `GreenhouseApiService` con un método para alertas no
resueltas (lo usa el counter):

```kotlin
suspend fun getUnresolvedAlerts(tenantId: Long): List<AlertResponse> {
    return httpClient.get("$baseUrl/alerts/unresolved/tenant/$tenantId").body()
}
```

Añade un método nuevo en el **mismo `GreenhouseApiService`** (no crees uno nuevo
si no es necesario — sigue el patrón del repo), o en un `AlertApiService`
dedicado si el usuario ya lo ha creado en su rama local. La firma:

```kotlin
import com.apptolast.greenhousefronts.data.model.alert.AlertDetailResponse
import io.ktor.client.request.parameter

/**
 * Full alert history for a tenant: includes both active and resolved alerts,
 * ordered by createdAt DESC. Backed by GET /api/v1/alerts/history/tenant/{id}
 * — the dedicated endpoint for the mobile "Histórico" tab. The legacy filter
 * `?isResolved=true` is intentionally NOT used here because it would only
 * return resolved alerts, leaving the tab empty until something gets resolved.
 */
suspend fun getAlertsHistory(
    tenantId: Long,
    limit: Int = 100,
): List<AlertDetailResponse> {
    return httpClient.get("$baseUrl/alerts/history/tenant/$tenantId") {
        parameter("limit", limit)
    }.body()
}
```

Notas:
- `$baseUrl` es la propiedad `val baseUrl: String` definida en
  `data/remote/KtorClient.kt` (devuelve `Environment.current.baseUrl`, que ya
  incluye el prefijo `/api/v1`).
- El cliente `httpClient` es el `AUTHENTICATED_CLIENT` (inyectado vía Koin en
  `di/DataModule.kt`). Bearer token automático.
- `import io.ktor.client.request.parameter` — el patrón con `parameter("...")`
  es el estándar Ktor para query strings; usa este en vez de concatenar
  manualmente.
- `expectSuccess = true` está activo: cualquier respuesta 4xx/5xx tira
  `ResponseException`. La capa repository ya lo maneja con `Result.failure`.

### Paso 3 — Repository

El cliente ya devuelve `Result<...>` desde la capa repositorio. Sigue el
patrón de `GreenhouseRepositoryImpl`:

```kotlin
// domain/repository/AlertRepository.kt (si no existe ya, créalo)
interface AlertRepository {
    /**
     * Returns the full alert history for the current tenant
     * (active + resolved), ordered by createdAt DESC.
     */
    suspend fun getAlertHistory(limit: Int = 100): Result<List<AlertDetail>>
}
```

```kotlin
// data/repository/AlertRepositoryImpl.kt
class AlertRepositoryImpl(
    private val apiService: GreenhouseApiService, // o AlertApiService si existe
    private val tokenStorage: TokenStorage,
) : AlertRepository {

    override suspend fun getAlertHistory(limit: Int): Result<List<AlertDetail>> {
        val tenantId = tokenStorage.getTenantId()
            ?: return Result.failure(Exception("No se encontró el ID del tenant"))

        return try {
            val dtos = apiService.getAlertsHistory(tenantId, limit)
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

Si Alberto tiene ya su propio modelo `domain/model/AlertDetail.kt` (o lo llama
`Alert`), úsalo. Si no, crea uno mínimo con solo los campos que la pantalla
Histórico necesita pintar.

Ejemplo de mapper en `data/model/alert/AlertMappers.kt` (extension function,
patrón consistente con el resto del repo):

```kotlin
fun AlertDetailResponse.toDomain(): AlertDetail = AlertDetail(
    id = id,
    code = code,
    sectorCode = sectorCode,
    severity = severityName,
    severityLevel = severityLevel,
    title = clientName ?: message ?: code,
    message = message,
    description = description,
    isResolved = isResolved,
    createdAt = createdAt,
    resolvedAt = resolvedAt,
)
```

### Paso 4 — ViewModel

Crea (o adapta el existente) un `AlertHistoryViewModel` siguiendo el patrón
de los demás ViewModels del repo (todos están en
`composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/presentation/viewmodel/`):

```kotlin
class AlertHistoryViewModel(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertHistoryUiState())
    val uiState: StateFlow<AlertHistoryUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        alertRepository.getAlertHistory().fold(
            onSuccess = { list ->
                _uiState.update {
                    it.copy(isLoading = false, alerts = list, error = null)
                }
            },
            onFailure = { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error desconocido")
                }
            },
        )
    }
}

data class AlertHistoryUiState(
    val isLoading: Boolean = false,
    val alerts: List<AlertDetail> = emptyList(),
    val error: String? = null,
)
```

### Paso 5 — Pantalla

Localiza la pantalla actual de Histórico (probablemente en
`presentation/ui/AlertsScreen.kt` o similar). Hoy debe estar llamando a algo
como `apiService.getAlerts(tenantId, isResolved = true, limit = 100)` o
construyendo manualmente la URL `?isResolved=true`. Sustitúyelo por una llamada
al nuevo `alertHistoryViewModel.uiState`.

Renderizado mínimo (Compose Multiplatform):
- Lista (LazyColumn) de tarjetas con: severidad (chip de color), `clientName`
  o `message`, fecha (formatear `createdAt`), y un badge **"Activa"** o
  **"Resuelta"** según `isResolved`.
- Si `error != null`, mostrar texto rojo con un botón Reintentar que llame a
  `viewModel.load()`.
- Si `isLoading && alerts.isEmpty()`, mostrar `CircularProgressIndicator`.
- Si la lista está vacía y no hay error, mostrar copy:
  *"Aún no se han generado alertas para este invernadero."*

### Paso 6 — Koin DI

Registra repository y viewmodel en los módulos correspondientes
(`di/DataModule.kt` y `di/PresentationModule.kt`):

```kotlin
// di/DataModule.kt — añadir junto al resto de repos
singleOf(::AlertRepositoryImpl) bind AlertRepository::class
```

```kotlin
// di/PresentationModule.kt — añadir junto a los demás VMs
viewModelOf(::AlertHistoryViewModel)
```

Si el usuario en su rama local **ya** ha registrado uno con otro nombre,
detéctalo y reúsa lo que tenga (no dupliques bindings).

### Paso 7 — Tests

El repo no tiene una cobertura amplia (solo
`composeApp/src/commonTest/kotlin/com/apptolast/greenhousefronts/ComposeAppCommonTest.kt`).
**No añadas tests de integración**, pero **sí** un test unitario del VM con
MockK / Turbine si ya están en el classpath (mirar
`gradle/libs.versions.toml`):

```kotlin
class AlertHistoryViewModelTest {
    @Test
    fun `loads alerts on init`() = runTest {
        val repo = mockk<AlertRepository>()
        coEvery { repo.getAlertHistory(any()) } returns Result.success(listOf(/* fake AlertDetail */))
        val vm = AlertHistoryViewModel(repo)
        vm.uiState.test {
            // assertions
        }
    }
}
```

Si Turbine / MockK no están en el `libs.versions.toml`, **no los añadas** —
solo dilo en tu reporte y deja el test fuera. Confirma que la app sigue
compilando para los 4 targets:

```bash
./gradlew :composeApp:assembleDebug                    # Android
./gradlew :composeApp:run                              # Desktop (JVM)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun      # Web (Wasm)
```

## Restricciones (estas son no-negociables)

- **NO** modifiques el `AlertResponse` recortado existente en
  `data/model/greenhouse/GreenhouseDtos.kt`. Si otro código lo usa para contar
  alertas (`GreenhouseRepositoryImpl.getGreenhouses` y `getGreenhouseDetail`),
  ese flujo debe seguir intacto.
- **NO** cambies la URL de la pestaña "Activas" (sigue siendo
  `?isResolved=false&limit=100` o el `getUnresolvedAlerts` actual del
  `GreenhouseApiService`).
- **NO** añadas un `AlertResponse` con el mismo nombre en otro paquete — usa
  un nombre distinto (`AlertDetailResponse` recomendado) para evitar
  ambigüedad de imports.
- **NO** desactives `isLenient`, `ignoreUnknownKeys`, ni `coerceInputValues`
  en `di/DataModule.kt`. Esa configuración es la que permite que el cliente
  tolere campos extra que el backend pueda añadir en el futuro.
- **NO** asumas que el path `/api/v1/alerts/history/...` está en algún
  documento OpenAPI local del repo cliente. **No lo está**. Confía en este
  documento — el endpoint está vivo en dev y prod (verificable con la URL
  pública).
- **Comentarios técnicos en inglés**, copy de UI en español (regla del
  CLAUDE.md del repo).
- **NO** alucines nada. Si te falta un dato (un nombre de archivo, un
  parámetro), pregúntale al usuario antes de inventar.

## Verificación

1. Build local:
   ```bash
   ./gradlew :composeApp:assembleDebug
   ```
   Sin warnings ni errores de Kotlin/serialization.

2. Ejecutar la app contra DEV (`Environment.current = Environment.DEV`),
   loguearse, ir a la pestaña "Histórico" del invernadero. Esperado:

   - **Antes** (bug actual): `[]` → pantalla vacía aunque haya alertas activas.
   - **Después** (con este fix): la lista contiene `ALT-00010` con badge
     "Activa" (porque `isResolved=false`). Si en el futuro alguien resuelve
     una alerta, también aparecerá con badge "Resuelta".

3. Logcat / consola del cliente:
   ```
   HttpClient: REQUEST: https://inverapi-dev.apptolast.com/api/v1/alerts/history/tenant/<ID>?limit=100
   HttpClient: RESPONSE: 200 OK
   BODY: [{"id":..., "code":"ALT-00010", "isResolved":false, ...}]
   ```
   Una sola petición. Sin 404, sin 500.

4. La pestaña **"Activas"** sigue mostrando lo mismo que antes (la misma
   alerta `ALT-00010` con badge crítico).

## Acceptance criteria

- [ ] Existe un DTO con todos los campos del response (`AlertDetailResponse` o
      el que el usuario ya tenga local).
- [ ] Hay un método `getAlertsHistory(tenantId, limit)` en `GreenhouseApiService`
      (o equivalente) que llama a `GET /alerts/history/tenant/{id}?limit=...`.
- [ ] Hay un `AlertRepository` (o un método nuevo en el existente) que devuelve
      `Result<List<AlertDetail>>`.
- [ ] Hay un `AlertHistoryViewModel` registrado en Koin.
- [ ] La pantalla "Histórico" consume el ViewModel y pinta la lista — no llama
      directamente al API.
- [ ] Pantalla "Activas" intacta.
- [ ] `./gradlew :composeApp:assembleDebug` pasa.
- [ ] Logcat muestra petición a `/alerts/history/tenant/{id}?limit=100` con
      200 y body no vacío en DEV.
- [ ] Yarn lock actualizado si añadiste deps (`./gradlew kotlinUpgradeYarnLock`
      y `./gradlew kotlinWasmUpgradeYarnLock`).

## Si encuentras algo distinto a lo descrito aquí

Detente y reporta. No reescribas la arquitectura del repo. Este prompt asume:
- MVVM + Repository (ya existente).
- Koin DI (ya existente).
- Ktor client autenticado (ya existente).
- kotlinx.serialization con `ignoreUnknownKeys` (ya configurado).

Si algo de eso ha cambiado, los snippets de arriba pueden no encajar 1:1 —
consulta antes de adaptar.

=== FIN PROMPT PARA CLAUDE CODE EN apptolast/GreenhouseFronts ===
```

---

## Anexos para Pablo (no para Alberto)

### A. Estado del backend (confirmado por mí)

- Commit que añade el endpoint: `01e4b4d` en `develop`.
- PR a main: https://github.com/apptolast/InvernaderosAPI/pull/107 (MERGED).
- Imágenes Docker:
  - `apptolast/invernaderos-api:develop` desplegado en
    `apptolast-invernadero-api-dev/invernaderos-api` (1 réplica), health 200.
  - `apptolast/invernaderos-api:latest` desplegado en
    `apptolast-invernadero-api-prod/invernaderos-api` (2 réplicas), health 200.
- Endpoint disponible en:
  - https://inverapi-dev.apptolast.com/api/v1/alerts/history/tenant/{id}
  - https://inverapi-prod.apptolast.com/api/v1/alerts/history/tenant/{id}
- Sin filtro `isResolved` (incluye activas y resueltas).
- Orden: `createdAt DESC`.
- Default `limit = 100`.
- Cero `LazyInitializationException` en logs tras rollout.

### B. Patrones del repo `GreenhouseFronts` que cito en el prompt

Verificados leyendo la rama `feature/alert-notifications` (HEAD `d81f181`):

- `composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/data/remote/api/GreenhouseApiService.kt`
  — patrón Ktor: `httpClient.get("$baseUrl/path/$param").body()`.
- `composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/data/remote/KtorClient.kt`
  — `baseUrl` y `createAuthenticatedHttpClient`.
- `composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/di/DataModule.kt`
  — config JSON con `ignoreUnknownKeys = true`, registro Koin.
- `composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/di/PresentationModule.kt`
  — `viewModelOf(...)`.
- `composeApp/src/commonMain/kotlin/com/apptolast/greenhousefronts/data/repository/GreenhouseRepositoryImpl.kt`
  — patrón `Result<T>` + tokenStorage para tenantId.
- `CLAUDE.md` raíz — reglas "no inventar", build commands, yarn lock.

### C. Cosas que NO he tocado y NO debe tocar Claude Code en el cliente

- `GreenhouseApiService.getUnresolvedAlerts` — sigue usándose para contar
  alertas en las cards.
- `AlertResponse` recortado en `GreenhouseDtos.kt` — sigue siendo el DTO de
  los counters.
- Pestaña "Activas" — su URL sigue funcionando con el fix del 500 (PR #106).
- `spring.jpa.open-in-view`, LAZY annotations, `@JsonIgnore`, `@NamedEntityGraph`
  en backend — sin cambios.

# Auditoría sistema notificaciones push (FCM) — Fase 1

> **Estado**: Fase 1 (auditoría). No contiene propuestas de rediseño. Cada afirmación factual se cita con `path/al/fichero:N-M`. Donde algo no existe se escribe literalmente "no encontrado".
> 
> **Rama auditada**: `feat/alert-history-real` (base `develop`).
> 
> **Convención**: K8s manifests viven fuera del repo `InvernaderosAPI` (en `../k8s/11-api-dev/` y `../k8s/10-api-prod/`, hermanos del checkout). Las citas a esos ficheros usan ruta absoluta `/home/admin/companies/apptolast/invernaderos/k8s/...`.

---

## 1. Validación de los 11 hechos del prompt

### Hecho 1 — Listener de push: `AlertActivationPushListener`, reacciona a `AlertStateChangedEvent`, solo dispara cuando `to_resolved=false` (activación)

**Confirmado.**

- Clase: `src/main/kotlin/com/apptolast/invernaderos/features/push/infrastructure/adapter/output/AlertActivationPushListener.kt`.
- Decoradores: `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` (línea 47) + `@Transactional(value = "metadataTransactionManager", propagation = Propagation.REQUIRES_NEW, readOnly = true)` (líneas 48-52).
- Filtro de resolución (líneas 57-63):
  ```kotlin
  if (change.toResolved) {
      logger.debug("Alert {} state change is a RESOLUTION (toResolved=true) — push skipped", alert.code)
      return
  }
  ```
- Conclusión: solo se envía push cuando `change.toResolved == false`. Las resoluciones se descartan en la línea 57.

### Hecho 2 — `AlertStateChangedEvent` se publica desde `ResolveAlertUseCaseImpl` y `ApplyAlertMqttSignalUseCaseImpl`, en TODAS las transiciones (apertura y cierre)

**Confirmado en ambas direcciones.**

| Caso de uso | Método | Línea de `eventPublisher.publish(...)` | `toResolved` |
|---|---|---|---|
| `ResolveAlertUseCaseImpl` | `resolve()` | `ResolveAlertUseCaseImpl.kt:61` | `true` (línea 54) |
| `ResolveAlertUseCaseImpl` | `reopen()` | `ResolveAlertUseCaseImpl.kt:99` | `false` (línea 92) |
| `ApplyAlertMqttSignalUseCaseImpl` | `execute()` | `ApplyAlertMqttSignalUseCaseImpl.kt:98` | derivado de `AlertSignalDecision` (línea 72), puede ser `true` o `false` |

El publicador real es el adapter `AlertStateChangedEventPublisherAdapter.kt:14-15` que delega en `ApplicationEventPublisher`.

### Hecho 3 — Cliente FCM: SDK Firebase Admin para Java/Kotlin

**Confirmado y ampliado.**

- Dependencia: `build.gradle.kts:77` → `implementation("com.google.firebase:firebase-admin:9.5.0")`.
- Bean: `src/main/kotlin/com/apptolast/invernaderos/features/push/infrastructure/config/FirebaseConfig.kt:36-90`.
- Carga de credenciales (resolución en cascada, `FirebaseConfig.kt:84-89`):
  1. Property `firebase.service-account-path` (`@Value("\${firebase.service-account-path:}")`, línea 38). No declarada explícitamente en `application.yaml`, pero se sobrescribe vía env var `FIREBASE_SERVICE_ACCOUNT_PATH` o property override.
  2. Fallback a env var estándar `GOOGLE_APPLICATION_CREDENTIALS` (línea 86).
- Inicialización **eager** vía `@Bean fun firebaseApp()` (línea 44). El bean se crea en arranque.
- Idempotencia: si ya existe `FirebaseApp.DEFAULT_APP_NAME`, se reutiliza (líneas 55-59).
- Degradación graciosa: si no hay credenciales o falla la carga, **NO se cae la app** — se loggea `WARN` y se devuelve `null`. `FcmPushService.kt:46-52` detecta el `firebaseMessaging == null` y hace no-op.
- Inicialización **sin** `projectId` explícito (línea 63-65 del builder). Se infiere del `project_id` del JSON.

### Hecho 4 — Tokens FCM en una tabla, con migración Flyway, relación a `users`/`tenants`, metadata del dispositivo, endpoint REST y política de invalidación

**Parcialmente confirmado. Detalle:**

- Tabla: `metadata.push_tokens`. Migración: `src/main/resources/db/migration/V38__create_push_tokens_and_notify_push_flag.sql:22-30`.
  ```sql
  CREATE TABLE IF NOT EXISTS metadata.push_tokens (
      id           BIGSERIAL PRIMARY KEY,
      user_id      BIGINT      NOT NULL REFERENCES metadata.users(id)   ON DELETE CASCADE,
      tenant_id    BIGINT      NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,
      token        TEXT        NOT NULL UNIQUE,
      platform     VARCHAR(16) NOT NULL CHECK (platform IN ('ANDROID','IOS','WEB')),
      created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
  );
  ```
- Relación: `user_id` y `tenant_id`, ambos con `ON DELETE CASCADE`.
- Metadata del dispositivo guardada: solo `platform` y `last_seen_at`. **NO** se guarda modelo, OS, versión de la app, locale ni timezone (no encontrado en migración ni en entidad `PushToken.kt:30-53`).
- Unicidad: `UNIQUE (token)` (V38 línea 26). No hay UNIQUE compuesto `(user_id, token)`.
- Endpoints REST: `PushTokenController.kt:31-58`.
  - `POST /api/v1/push-tokens` (líneas 31-47) — registro/upsert. JWT obligatorio (`@SecurityRequirement(name = "bearerAuth")`, línea 24). `tenantId`/`userId` se resuelven desde `Authentication`, **nunca** del body (líneas 41-45).
  - `DELETE /api/v1/push-tokens/{token}` (líneas 49-58) — desregistro/revocación.
- DTO de request: `PushTokenRegisterRequest` con `token: String` y `platform: PushPlatform` (`enum { ANDROID, IOS, WEB }`).
- Política de invalidación ante `UNREGISTERED`/`INVALID_ARGUMENT`: **automática**. `FcmPushService.kt:76-82`:
  ```kotlin
  if (code == MessagingErrorCode.UNREGISTERED ||
      code == MessagingErrorCode.INVALID_ARGUMENT) {
      val deletedRows = pushTokenRepository.deleteByToken(deadToken)
      logger.info("Removed invalid FCM token (code={}) deletedRows={} alertCode={}", code, deletedRows, payload.alertCode)
  }
  ```
- Otros códigos de error FCM: solo se loggean `WARN` (`FcmPushService.kt:84-90`); el token NO se borra.
- Scheduler de housekeeping de tokens viejos por `last_seen_at`: **no encontrado**. El comentario en `PushToken.kt:18-19` lo deja explícito ("permite limpiar tokens huérfanos en una rutina de mantenimiento futura — no implementada aquí").

### Hecho 5 — Selección de destinatarios cuando dispara una alerta

**Confirmado: a TODOS los tokens del tenant.**

- Punto de entrada: `AlertActivationPushListener.kt:111` → `fcmPushService.sendAlertToTenant(payload)`.
- En `FcmPushService.kt:54`: `val tokens = pushTokenRepository.findAllByTenantId(payload.tenantId).map { it.token }`.
- Query: `PushTokenRepository.kt:14` → `fun findAllByTenantId(tenantId: Long): List<PushToken>` (Spring Data derived query, sin filtro de rol, sector ni invernadero).
- Conclusión: **todos los tokens registrados del tenant** reciben el push, sin filtrado por sector, invernadero o rol del usuario.

### Hecho 6 — Payload FCM actual

**Confirmado e inventariado completo.** Detalles en sección 1.2.

Construcción: `FcmPushService.kt:115-156`.

- `notification.title` y `notification.body`: hardcoded en español, sin i18n. Origen del título: `AlertActivationPushListener.kt:103` → `"Nueva alerta: ${severity.name}"`.
- `data.*`: 7 claves técnicas (alertId, alertCode, greenhouseId, sectorId, severity, severityLevel, createdAt), líneas 127-133.
- Android (líneas 134-144): `priority=HIGH`, `channelId="alerts_default"` (constante línea 169), `color` desde severity o default `#00E676` (línea 171). **Sin `setSound`** en Android.
- APNS (líneas 145-154): `setSound("default")`, `setContentAvailable(true)`. iOS sí está cubierto.
- WebPush: **no encontrado**. No hay `.setWebpushConfig(...)` en el builder. Clientes navegador reciben payload sin tunear.
- Localización por idioma del usuario: **no encontrado** (no hay `MessageSource`, no hay bundles `messages_*.properties`, no hay locale del usuario — ver hechos 8 y aparte).

### Hecho 7 — Severidades y `notify_push`: el listener lo respeta en activaciones

**Confirmado en activación. La pregunta sobre resolución no aplica al estado actual** (las resoluciones no envían push: ya se descartan en `AlertActivationPushListener.kt:57` antes de leer la severidad).

- Columna en entidad: `AlertSeverity.kt:45-53`:
  ```kotlin
  @Column(name = "notify_push", nullable = false)
  val notifyPush: Boolean = true,
  ```
- Migración: `V38:17-18` → `ALTER TABLE metadata.alert_severities ADD COLUMN IF NOT EXISTS notify_push BOOLEAN NOT NULL DEFAULT TRUE;`.
- Lectura en listener: `AlertActivationPushListener.kt:77-83`:
  ```kotlin
  if (!severity.notifyPush) {
      logger.debug("Severity {} has notify_push=false — push skipped for alert {}", severity.name, alert.code)
      return
  }
  ```
- Estado: el flag se respeta en la única ruta que envía push (activación). No hay segunda ruta donde aplicarlo.

### Hecho 8 — Preferencias por usuario (UserNotificationPreferences)

**No encontrado.**

- No existe migración Flyway con tabla `user_notification_preferences`, `notification_settings` ni equivalente (verificado con búsqueda en `src/main/resources/db/migration/V*.sql`).
- No existe entidad JPA equivalente.
- No existe endpoint REST `/api/v1/users/me/notification-preferences` ni similar.
- Único control granular existente: `alert_severities.notify_push` (admin-side, no per-user).

### Hecho 9 — Otros listeners que reaccionan a eventos de alerta

**Confirmado, 4 listeners en total para `AlertStateChangedEvent`** (paralelos, independientes):

| Clase | Archivo:línea | Decorador | Acción |
|---|---|---|---|
| `AlertActivationPushListener` | `features/push/infrastructure/adapter/output/AlertActivationPushListener.kt:47` | `@TransactionalEventListener(AFTER_COMMIT)` | FCM push, solo activaciones. |
| `TenantStatusBroadcastListener.onAlertStateChanged` | `features/websocket/broadcast/TenantStatusBroadcastListener.kt:63` | `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` | Broadcast WebSocket genérico por tenant en AMBAS transiciones (snapshot). Source `ALERT_ACTIVATED`/`ALERT_RESOLVED` (líneas 70, 98-99). |
| `AlertStateChangedWebSocketListener` | `features/alert/infrastructure/adapter/output/AlertStateChangedWebSocketListener.kt:30` | `@TransactionalEventListener(AFTER_COMMIT)` | WebSocket dirigido a `/topic/tenant/{tenantId}/alerts` con `AlertTransition` derivado in-memory (sin re-query). En AMBAS transiciones. |
| `AlertStateChangedMqttEchoListener` | `features/alert/infrastructure/adapter/output/AlertStateChangedMqttEchoListener.kt:55` | `@TransactionalEventListener(AFTER_COMMIT)` | Eco MQTT a `GREENHOUSE/RESPONSE` en AMBAS transiciones (con kill-switch `alert.mqtt.echo.enabled`). |

**No existe listener de push para resoluciones.** Es el hueco principal del sistema actual.

### Hecho 10 — Inventario de `ApplicationEvent`s del sistema (no solo alertas)

**Confirmado: solo 3 clases de evento en todo el repo.** Detalle completo en sección 1.4.

| Evento | Archivo:línea | Dominio |
|---|---|---|
| `AlertStateChangedEvent` | `features/alert/infrastructure/adapter/output/AlertStateChangedEvent.kt:10-13` | Alertas |
| `DeviceCurrentValuesFlushedEvent` | `features/websocket/event/DeviceCurrentValuesFlushedEvent.kt:18-20` | Telemetría |
| `TenantStatusChangedEvent` | `features/websocket/event/TenantStatusChangedEvent.kt:16-28` | CRUD multi-dominio (greenhouses, sectors, devices, settings, users) |

De los tres, **solo `AlertStateChangedEvent` desemboca en envío FCM**.

### Hecho 11 — Logging y observabilidad del envío FCM

**Confirmado. Sin reintento, sin DLQ.**

- Log de envío:
  - `FcmPushService.kt:47-50` (DEBUG) — FCM deshabilitado, no-op.
  - `FcmPushService.kt:56-59` (INFO) — sin tokens registrados.
  - `FcmPushService.kt:79-82` (INFO) — token inválido borrado.
  - `FcmPushService.kt:84-90` (WARN) — fallo individual no recuperable.
  - `FcmPushService.kt:99-102` (ERROR + exception) — multicast call falló entero.
  - `FcmPushService.kt:109-112` (INFO) — resumen final por envío (tenantId, alertCode, tokens, success, failed).
- Métricas Micrometer:
  - `meterRegistry.counter("push.fcm.failed", "reason", code?.name ?: "UNKNOWN").increment()` (línea 92-95).
  - `meterRegistry.counter("push.fcm.failed", "reason", "EXCEPTION").increment()` (línea 104).
  - `meterRegistry.counter("push.fcm.sent").increment(totalSuccess.toDouble())` (línea 108).
- Reintento ante error transitorio: **no encontrado**. Sin `@Retryable`, `@CircuitBreaker`, ni bucle interno.
- Dead-letter queue / outbox / Redis Streams: **no encontrado**.

---

## 1.1 Mapa actual del flujo de notificación push

```
                       ┌─────────────────────────────────────────┐
                       │ Origen 1: API REST                       │
                       │  AlertController → ResolveAlertUseCase   │
                       │  (resolve toResolved=true                │
                       │   reopen  toResolved=false)              │
                       └──────────────────┬──────────────────────┘
                                          │
                       ┌─────────────────────────────────────────┐
                       │ Origen 2: MQTT GREENHOUSE/STATUS         │
                       │  DeviceStatusProcessor.processStatus     │
                       │   .kt:91 if code.startsWith("ALT-")      │
                       │  → AlertMqttInboundAdapter               │
                       │  → ApplyAlertMqttSignalUseCaseImpl       │
                       │  (toResolved derivado de AlertSignal-    │
                       │   DecisionAdapter, puede ser true/false) │
                       └──────────────────┬──────────────────────┘
                                          │
                                          ▼
              ┌──────────────────────────────────────────────────┐
              │ alertRepository.save(alert)                       │
              │ stateChangePort.save(AlertStateChange + actor)    │
              │ eventPublisher.publish(AlertStateChangedEvent)    │
              │       (en transacción metadataTransactionManager) │
              └──────────────────┬───────────────────────────────┘
                                 │
                                 │ COMMIT
                                 │
              ┌──────────────────┴───────────────────────────────┐
              │ Spring delivery a 4 listeners AFTER_COMMIT:      │
              │                                                   │
              │  ┌────────────────────────────┐                  │
              │  │ AlertActivationPushListener│ ──► FCM          │
              │  │ .kt:57 if (toResolved)     │      ✓ activación│
              │  │   return                   │      ✗ resolución│
              │  └────────────────────────────┘                  │
              │                                                   │
              │  ┌────────────────────────────┐                  │
              │  │ TenantStatusBroadcast      │ ──► WS snapshot  │
              │  │ Listener.onAlertState      │      ✓ ambas     │
              │  │ Changed (broadcast.kt:63)  │                  │
              │  └────────────────────────────┘                  │
              │                                                   │
              │  ┌────────────────────────────┐                  │
              │  │ AlertStateChangedWebSocket │ ──► WS topic     │
              │  │ Listener.kt:30             │   /tenant/{id}/  │
              │  │ → /topic/tenant/{id}/alerts│       alerts     │
              │  └────────────────────────────┘      ✓ ambas     │
              │                                                   │
              │  ┌────────────────────────────┐                  │
              │  │ AlertStateChangedMqttEcho  │ ──► MQTT echo   │
              │  │ Listener.kt:55 (defensivo  │      ✓ ambas     │
              │  │ + kill-switch)             │      (transición)│
              │  └────────────────────────────┘                  │
              └──────────────────────────────────────────────────┘
                                 │
                                 ▼
              ┌──────────────────────────────────────────────────┐
              │ FcmPushService.sendAlertToTenant (kt:44)          │
              │  - findAllByTenantId(tenantId)                    │
              │  - chunked(500) → sendEachForMulticast            │
              │    (síncrono, dentro de @Transactional)           │
              │  - UNREGISTERED/INVALID_ARGUMENT → deleteByToken │
              │  - Otros errores → log WARN, sin retry           │
              │  - Excepción → log ERROR, sin retry              │
              └──────────────────────────────────────────────────┘
```

**Cortes del flujo** (oportunidades de Fase 2, no se implementan ahora):

- **Resolución de alerta → FCM**: cortado deliberadamente en `AlertActivationPushListener.kt:57`. El operario que vio la alerta encenderse no recibe push de cierre.
- **Otros eventos de dominio (`TenantStatusChangedEvent`, `DeviceCurrentValuesFlushedEvent`) → FCM**: ningún listener FCM los escucha. Solo desembocan en WebSocket.
- **Eventos de "dispositivo offline / gateway caído / sensor sin lecturas / alerta sin resolver tras X / suscripción que caduca"**: no existen como evento publicado en el código (ver sección 1.4).

---

## 1.2 Inventario del payload FCM enviado hoy

Construido en `FcmPushService.buildMulticastMessage` (`FcmPushService.kt:115-156`).

| Clave | Tipo | Valor / Origen | Consumida por cliente | Localizada |
|---|---|---|---|---|
| `notification.title` | String | `"Nueva alerta: ${severity.name}"` (`AlertActivationPushListener.kt:103`) | Sí, visible en notificación | **No** (hardcoded ES) |
| `notification.body` | String | Primer no-nulo de: `alert.message` ?: `alert.description` ?: `alert.clientName` ?: `alert.code` (`AlertActivationPushListener.kt:104-107`) | Sí, visible en notificación | **No** (texto del operador, sin traducción) |
| `data.alertId` | String (de Long) | `payload.alertId.toString()` | Probable: deep-link a detalle de alerta | n/a |
| `data.alertCode` | String | `payload.alertCode` (ej. `ALT-00010`) | Probable: identificador para correlación | n/a |
| `data.greenhouseId` | String (de Long) | `payload.greenhouseId.toString()` | Probable: navegación al invernadero | n/a |
| `data.sectorId` | String (de Long) | `payload.sectorId.toString()` | Probable: navegación al sector | n/a |
| `data.severity` | String | `severity.name` (ej. `CRITICAL`) | Probable: render del color/icono en notificación | **No** (literal del catálogo) |
| `data.severityLevel` | String (de Short) | `severity.level.toString()` | Probable: ordenación en cliente | n/a |
| `data.createdAt` | String (epoch millis) | `alert.createdAt.toEpochMilli().toString()` | Probable: timestamp en UI | n/a |
| `android.priority` | Enum | `AndroidConfig.Priority.HIGH` | n/a (sistema operativo) | n/a |
| `android.notification.channelId` | String | `"alerts_default"` (`FcmPushService.kt:169`) | Sí: el canal debe existir en el cliente | n/a |
| `android.notification.color` | String hex | `payload.severityColor ?: "#00E676"` (línea 140 + constante 171) | Sí: color del icono | n/a |
| `apns.aps.sound` | String | `"default"` | Sí: sonido del sistema | n/a |
| `apns.aps.contentAvailable` | Boolean | `true` | Sí: permite background processing en iOS | n/a |
| **`webpush.*`** | — | **no encontrado** | — | — |

---

## 1.3 Modelo de datos actual de tokens y preferencias

### 1.3.a Tabla de tokens FCM

```sql
-- src/main/resources/db/migration/V38__create_push_tokens_and_notify_push_flag.sql:22-30
CREATE TABLE IF NOT EXISTS metadata.push_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES metadata.users(id)   ON DELETE CASCADE,
    tenant_id    BIGINT      NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,
    token        TEXT        NOT NULL UNIQUE,
    platform     VARCHAR(16) NOT NULL CHECK (platform IN ('ANDROID','IOS','WEB')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- líneas 32-33
CREATE INDEX IF NOT EXISTS idx_push_tokens_tenant ON metadata.push_tokens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_push_tokens_user   ON metadata.push_tokens(user_id);
```

Entidad JPA: `features/push/PushToken.kt:30-53`. Repositorio: `features/push/PushTokenRepository.kt:10-19`.

**Metadatos del dispositivo NO almacenados** (no encontrado): modelo, OS, versión de la app, locale del usuario, timezone, fingerprint del navegador para WEB.

### 1.3.b Tabla de preferencias de notificación por usuario

**No encontrado.** No existe tabla `user_notification_preferences`, `notification_settings` ni equivalente en `src/main/resources/db/migration/V*.sql`. No existe entidad JPA. No existe endpoint.

### 1.3.c Idioma / locale del usuario

**No encontrado.** La entidad `features/user/User.kt:17-50` no contiene columna `locale`, `language` ni `preferred_language`. La tabla `metadata.users` de las migraciones tampoco la incluye.

### 1.3.d Política de housekeeping de tokens muertos

- **Auto-invalidación reactiva**: sí. `FcmPushService.kt:76-82` borra el token cuando FCM responde `UNREGISTERED` o `INVALID_ARGUMENT`.
- **Cleanup proactivo por `last_seen_at` antiguo**: **no encontrado**. Comentario explícito en `PushToken.kt:18-19` reconoce el hueco ("rutina de mantenimiento futura — no implementada aquí").

---

## 1.4 Inventario de `ApplicationEvent`s candidatos a notificación

Búsqueda exhaustiva en `src/main/kotlin/**`. Solo 3 clases de evento existen.

### 1.4.a Eventos existentes

| # | Evento | Archivo:línea | Dominio | Publicadores | Listeners | ¿Push hoy? | Valoración |
|---|---|---|---|---|---|---|---|
| 1 | `AlertStateChangedEvent(alert, change)` | `features/alert/infrastructure/adapter/output/AlertStateChangedEvent.kt:10-13` | Alertas | `ResolveAlertUseCaseImpl.kt:61` (resolve), `:99` (reopen); `ApplyAlertMqttSignalUseCaseImpl.kt:98` (MQTT, ambas direcciones); todos vía `AlertStateChangedEventPublisherAdapter.kt:14-15` | `AlertActivationPushListener.kt:47` (FCM, solo activación); `TenantStatusBroadcastListener.kt:63` (WS snapshot, ambas); `AlertStateChangedWebSocketListener.kt:30` (WS topic alerta, ambas); `AlertStateChangedMqttEchoListener.kt:55` (MQTT echo, ambas) | **Sí, parcialmente** (solo activación) | **Candidato a ampliar**: ya está cableado, falta listener de resolución. |
| 2 | `DeviceCurrentValuesFlushedEvent(tenantIds: Set<Long>)` | `features/websocket/event/DeviceCurrentValuesFlushedEvent.kt:18-20` | Telemetría | `DeviceStatusProcessor.kt:136-138` (en `flushCurrentValues`, dentro de `@Scheduled(fixedRate=1000)`) | `TenantStatusBroadcastListener.kt:45` (WS) | No | **No candidato**: cadencia ≤1/s/tenant, ráfaga continua. Convertir a push generaría spam. |
| 3 | `TenantStatusChangedEvent(tenantId, source: enum)` con `Source = {ALERT, GREENHOUSE_CRUD, SECTOR_CRUD, DEVICE_CRUD, SETTING_CRUD, USER_CRUD}` | `features/websocket/event/TenantStatusChangedEvent.kt:16-28` | CRUD multi-dominio | 15 publicadores (3 por tipo: create/update/delete × {Greenhouse, Sector, Device, Setting, User}). Ej. `CreateGreenhouseUseCaseImpl.kt:40`, `UpdateUserUseCaseImpl.kt:51`, etc. | `TenantStatusBroadcastListener.kt:80` (WS) | No | **Dudoso, requiere decisión de producto**: notificar push por cada CRUD admin probablemente no aporta valor al operario. Caso a evaluar: sub-eventos específicos (ej. "se ha creado un nuevo invernadero asignable") podrían justificar push, pero NO en su forma actual de granularidad. |

### 1.4.b Casos sugeridos por el prompt — qué hay y qué no

| Caso del prompt | Evento publicado en código | Job/scheduler que lo detecte | Estado |
|---|---|---|---|
| Dispositivos offline | **no encontrado** | **no encontrado** (no hay heartbeat ni timeout en `metadata.devices`) | Implementarlo en Fase 2 requiere: scheduler de detección + nuevo evento + listener FCM |
| Gateways caídos | **no encontrado** (la arquitectura es MQTT-only sin entidad `gateway` persistida) | **no encontrado** | Como arriba; además, requiere decidir si modelar `gateways` |
| Sensores sin lecturas en X tiempo | **no encontrado** | **no encontrado** | Detector basado en `last_seen_at` de `device_current_values` o equivalente |
| Alertas activas sin resolver tras Y tiempo (recordatorio escalado) | **no encontrado** | **no encontrado** | Scheduler que recorra `metadata.alerts WHERE is_resolved=false AND created_at < now() - INTERVAL X` |
| Suscripciones que caducan | **no encontrado** (no hay tabla `subscriptions` en migraciones) | **no encontrado** | Requiere modelar suscripciones primero |
| Resolución automática por timeout | **no encontrado** | **no encontrado** | n/a |

**Conclusión 1.4**: la única ampliación con coste bajo y de alto valor que **ya tiene el evento publicado** es la notificación de **resolución de alerta**. Todo lo demás requiere primero crear el detector + el evento.

### 1.4.c Schedulers existentes en el repo

Único `@Scheduled` encontrado: `mqtt/service/DeviceStatusProcessor.kt:101` → `flushPendingChanges()` con `fixedRate = 1000`. Su responsabilidad es flush de buffers a TimescaleDB; emite `DeviceCurrentValuesFlushedEvent` cuando hay tenants afectados (líneas 136-138). No hay otros schedulers en `src/main/kotlin`.

### 1.4.d Otros listeners reactivos en el repo

Lista completa de `@TransactionalEventListener` y `@EventListener`:

| Listener | Archivo:línea | Evento |
|---|---|---|
| `AlertActivationPushListener.onAlertActivated` | `features/push/.../AlertActivationPushListener.kt:47` | `AlertStateChangedEvent` |
| `TenantStatusBroadcastListener.onDeviceCurrentValuesFlushed` | `features/websocket/broadcast/TenantStatusBroadcastListener.kt:45` | `DeviceCurrentValuesFlushedEvent` |
| `TenantStatusBroadcastListener.onAlertStateChanged` | `features/websocket/broadcast/TenantStatusBroadcastListener.kt:63` | `AlertStateChangedEvent` |
| `TenantStatusBroadcastListener.onTenantStatusChanged` | `features/websocket/broadcast/TenantStatusBroadcastListener.kt:80` | `TenantStatusChangedEvent` |
| `AlertStateChangedWebSocketListener.onAlertStateChanged` | `features/alert/.../AlertStateChangedWebSocketListener.kt:30` | `AlertStateChangedEvent` |
| `AlertStateChangedMqttEchoListener.onAlertStateChanged` | `features/alert/.../AlertStateChangedMqttEchoListener.kt:55` | `AlertStateChangedEvent` |

Total: 6 métodos listener, 4 clases.

---

## 1.5 Riesgos y deuda detectados

> Solo enumerar — sin propuesta de fix.

### Hallazgo del prompt corregido — service-account JSON

El prompt me pidió comprobar si "Service account de Firebase: ¿está hardcodeado en repo, en variable de entorno, en secret de Kubernetes?". El hallazgo es **positivo**:

- El fichero `greenhouse-fronts-firebase-adminsdk-fbsvc-2db764766f.json` existe en la raíz del repo (working tree local) **pero está correctamente gitignored** en `.gitignore:54`:
  ```
  ### Firebase Admin SDK service account (NEVER COMMIT) ###
  *firebase-adminsdk*.json
  greenhouse-fronts-firebase-adminsdk-*.json
  firebase-service-account*.json
  ```
- Verificación: `git ls-files greenhouse-fronts-firebase-adminsdk-fbsvc-2db764766f.json` devuelve vacío (no tracked); `git log` sobre el path tampoco devuelve commits.
- En entornos K8s (dev y prod) las credenciales se inyectan vía secret externo `firebase-admin-sa`, montado read-only en `/run/secrets/firebase/service-account.json`:
  - Dev: `/home/admin/companies/apptolast/invernaderos/k8s/11-api-dev/03-deployment.yaml:105-177` (env var `GOOGLE_APPLICATION_CREDENTIALS` línea 109-110, volumen `firebase-sa` línea 119-120 + 166-177).
  - Prod: `/home/admin/companies/apptolast/invernaderos/k8s/10-api-prod/03-deployment.yaml:106-178` (mismo patrón, líneas 110-111 y 167-178).
- `application.yaml` también está gitignored (`/src/main/resources/application.yaml`), lo cual evita filtrar passwords por defecto al repo.

**Estado**: hygiene correcta. El JSON local en la raíz es para desarrollo y nunca llega a git. No se considera riesgo crítico.

### Riesgos reales detectados

1. **Envío FCM síncrono dentro de transacción de alerta.** `FcmPushService.kt:44` declara `@Transactional("metadataTransactionManager")` y los chunks se envían bloqueantes vía `messaging.sendEachForMulticast(message)` (línea 68). Como el listener abre `Propagation.REQUIRES_NEW` (`AlertActivationPushListener.kt:50`) la transacción de la alerta original ya está cerrada — pero la transacción del listener queda abierta durante todo el envío FCM. Si FCM se ralentiza, se mantiene una conexión JDBC retenida + el thread bloqueado. Sin `@Async` ni executor dedicado.

2. **Sin reintento ni cola persistente.** Si `messaging.sendEachForMulticast` lanza excepción, `FcmPushService.kt:98-105` la captura, suma el chunk a `totalFailed` e incrementa `push.fcm.failed{reason=EXCEPTION}` — pero **no reintenta** y **no persiste** la notificación pendiente. Notificación perdida sin trazabilidad.

3. **Sin tabla de log append-only de notificaciones enviadas.** La auditoría existe para transiciones de alerta (`metadata.alert_state_changes` con columnas de actor en V37 + V40, mapeadas en `AlertStateChange.kt:43-50` y `AlertMappers.kt:107-144`) pero NO para envíos FCM. No se puede responder desde la BD: "¿llegó esta notificación al usuario X el día Y?". Solo quedan logs efímeros + métricas agregadas.

4. **Sin internacionalización del payload.** `AlertActivationPushListener.kt:103` construye literal `"Nueva alerta: ${severity.name}"`; `severity.name` es el name del catálogo (`CRITICAL`, `WARNING`, etc.) sin traducir; `body` toma texto del operador sin pasar por bundle. No hay `MessageSource` ni `messages_*.properties`. Por tanto el operario con móvil en inglés ve español + un literal de severidad sin traducir.

5. **Sin deduplicación / coalescencia.** Una alerta con sensor que oscila alrededor del umbral generará N transiciones MQTT en pocos segundos → N pushes. Sin cache de "ya enviado en los últimos X segundos" (Redis disponible en stack pero no usado para esto).

6. **Sin quiet hours / preferencias por usuario.** No existe tabla de preferencias (sección 1.3.b). El admin solo puede silenciar a nivel severidad (binario, all-tenants).

7. **Selección de destinatarios sin filtrado.** `findAllByTenantId` (`PushTokenRepository.kt:14`) devuelve todos los tokens del tenant sin importar:
   - Si el usuario está asignado al sector o invernadero de la alerta.
   - Si el usuario tiene rol relevante.
   - Si el usuario ya está usando la app activamente (no aplica filtro).
   Dato a destacar: el payload sí incluye `greenhouseId` y `sectorId` (`FcmPushService.kt:129-130`), por tanto el cliente Android puede filtrar localmente, pero el backend no filtra al enviar.

8. **Sin webpush config.** `FcmPushService.kt:115-156` configura `setAndroidConfig` y `setApnsConfig` pero no `setWebpushConfig`. La plataforma `WEB` está aceptada en la migración (`V38:27`) pero recibe payload sin tunear (sin TTL, sin `Urgency`, sin `actions`).

9. **Códigos `ALT-` desconocidos en BD se descartan en silencio.** `ApplyAlertMqttSignalUseCaseImpl.kt:41-46` loggea `WARN` y devuelve `Either.Left(AlertError.UnknownCode)`; el flujo MQTT no lo eleva. Si el firmware empieza a emitir un `ALT-` nuevo antes de que admin cree el catálogo, ese tráfico se pierde sin alerta operativa. (No es bug, es comportamiento documentado, pero a flagar como deuda.)

10. **Sin housekeeping proactivo de tokens.** `last_seen_at` se actualiza pero no hay scheduler que purgue tokens >N días sin actividad. Confirmado por comentario explícito en `PushToken.kt:18-19`.

11. **Concurrencia de transiciones de alerta sin locking.** Documentado por el equipo en `AlertStateChangedMqttEchoListener.kt:40-45`: la tabla `alerts` no tiene `@Version`, dos `/resolve` simultáneos sobre la misma alerta pueden ambos crear `AlertStateChange(API)` y ambos disparar push. **No es un bug nuevo** — el equipo eligió aceptarlo a cambio de evitar el coste de optimistic locking. Para el dominio de notificaciones implica que un mismo evento puede materializarse en duplicado downstream. Relevante para la dedup.

### TODOs / FIXMEs

Búsqueda `grep -rn "TODO.*push\|TODO.*fcm\|TODO.*firebase\|TODO.*notification\|FIXME.*push" src/main/kotlin`: **no encontrado**. Solo aparece un TODO incidental en `AlertController.kt:44` sobre `@CrossOrigin`, no relacionado.

---

## Cierre Fase 1

Auditoría completada. Los 11 hechos del prompt han sido validados contra el código:

- **Hechos confirmados sin matiz**: 1, 2, 3, 5, 6, 7, 9, 10, 11.
- **Hechos confirmados con corrección/ampliación**: 4 (metadata limitada, sin housekeeping proactivo).
- **Hechos negativos confirmados**: 8 (no hay preferencias por usuario, no hay locale).

Hallazgos relevantes para la Fase 2 que el prompt no anticipaba:

- El listener `TenantStatusBroadcastListener.onAlertStateChanged` (`features/websocket/broadcast/TenantStatusBroadcastListener.kt:63-78`) **ya distingue** activación vs resolución (`SOURCE_ALERT_ACTIVATED` / `SOURCE_ALERT_RESOLVED`). Hay precedente arquitectónico para añadir un listener FCM equivalente al lado, sin tocar el evento ni los publicadores.
- La columna `actor_*` de `alert_state_changes` **ya está mapeada** en la entidad JPA (`AlertStateChange.kt:43-50`) y en los mappers (`AlertMappers.kt:107-144`), y los use-cases ya populan el actor (`ResolveAlertUseCaseImpl.kt:50-59`, `ApplyAlertMqttSignalUseCaseImpl.kt:85-94`). Esto es relevante para el cuerpo del push de resolución (poder decir "resuelta por X usuario" o "resuelta automáticamente vía MQTT" sin trabajo extra).
- El bug "MQTT ignora `ALT-XXXXX`" mencionado en el prompt **no existe**. El filtro `code.startsWith("ALT-")` en `DeviceStatusProcessor.kt:91` enruta correctamente. Confirmado por el usuario en aclaración previa.
- El service-account JSON **no está commiteado**. Está gitignored y se inyecta vía secret de K8s en dev y prod. Hygiene correcta.

**Esperando confirmación del usuario para abrir Fase 2.** Hasta entonces no se tocará código (`src/main/kotlin/**`), no se añadirán migraciones (`V41+`), ni se modificarán manifests K8s.

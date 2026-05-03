# Rediseño sistema notificaciones push (FCM) — Fase 2

> **Estado**: Fase 2 (diseño). Cero código todavía. Implementación + despliegue (Fase 3) requiere aprobación explícita después de revisar este documento.
>
> **Documento previo**: `docs/architecture/push-notifications-audit.md` (Fase 1, auditoría del estado actual).
>
> **Premisa**: mantener todo lo que ya funciona, ampliar cobertura de casos útiles, dejar el sistema preparado para que un usuario configure qué le notifica y cuándo. Cambios aditivos; lo existente solo se modifica donde se justifica explícitamente.

---

## 0. Contexto y decisiones de alcance

### 0.1 Resumen de la auditoría Fase 1

Sistema actual:
- 1 evento `AlertStateChangedEvent`, publicado en TODAS las transiciones (`ResolveAlertUseCaseImpl.kt:61, :99` + `ApplyAlertMqttSignalUseCaseImpl.kt:98`).
- 4 listeners reactivos. Solo 1 desemboca en FCM: `AlertActivationPushListener.kt:47`, filtrado a activación (`if (change.toResolved) return`, línea 57).
- Severidad respeta flag `notify_push` (`AlertActivationPushListener.kt:77`).
- Recipient selection: todos los tokens del tenant (`PushTokenRepository.findAllByTenantId`).
- Sin preferencias por usuario, sin locale, sin i18n, sin dedup, sin retry, sin DLQ, sin tabla append-only de log de notificaciones, sin housekeeping proactivo de tokens.
- Hygiene de secrets correcta: service-account JSON gitignored (`.gitignore:54`); secret K8s `firebase-admin-sa` montado en dev y prod (`.../11-api-dev/03-deployment.yaml:109-177` y `.../10-api-prod/03-deployment.yaml:110-178`).

### 0.2 Decisiones tomadas (usuario, sesión actual)

| Decisión | Resultado |
|---|---|
| **Tabla append-only de log de notificaciones** (sección F del prompt) | **En MVP**: migración + entidad + writer + endpoint paginado `GET /api/v1/users/me/notifications`. |
| **Recordatorio escalado** (alerta activa sin resolver tras X tiempo) | **En alcance**: scheduler nuevo + evento `AlertAgingDetectedEvent` + listener FCM. Threshold por severidad, configurable. |
| **Estructura del código nuevo** | **Módulo nuevo `features/notification/` hexagonal estricto** (cumple CLAUDE.md y precedente de `features/alert/`). El módulo `push` legacy queda como adapter de salida (FcmPushService inyectado). |

### 0.3 Notificaciones obligatorias ya pre-aprobadas

| Tipo | Trigger | Estado |
|---|---|---|
| `ALERT_ACTIVATED` | `AlertStateChangedEvent` con `change.toResolved == false` | Funciona hoy. Mantener. |
| `ALERT_RESOLVED` | `AlertStateChangedEvent` con `change.toResolved == true` | Nuevo. Confirmado por usuario. |
| `ALERT_AGING` | `AlertAgingDetectedEvent` (nuevo, emitido por scheduler) | Nuevo. Confirmado por usuario. |

---

## A — Catálogo de notificaciones

### A.1 Existente (mantener sin cambios funcionales, refactor para que pase por el nuevo módulo `notification`)

#### A.1.1 `ALERT_ACTIVATED`

| Campo | Valor |
|---|---|
| **Trigger** | `AlertStateChangedEvent` (`AlertStateChangedEvent.kt:10-13`) con `change.toResolved == false` |
| **Audiencia** | Todos los tokens del tenant cuya preferencia `categoryAlerts == true` AND `minSeverity <= alert.severity.level` AND no en `quiet_hours` (filtro nuevo) |
| **Severidad / urgencia** | Reaprovecha `alert.severityId` (`AlertSeverity.level`). Respeta `severity.notify_push` flag a nivel admin (no per-user). |
| **Título** | `notification.alert.activated.title` con placeholder `{severityName}` (i18n) → ej. `"Nueva alerta: CRITICAL"` (es-ES), `"New alert: CRITICAL"` (en-US) |
| **Cuerpo** | Igual que hoy: primer no-nulo de `alert.message`, `alert.description`, `alert.clientName`, `alert.code` (texto del operador, NO se traduce) |
| **Payload `data`** | Igual al actual (alertId, alertCode, greenhouseId, sectorId, severity, severityLevel, createdAt) + nuevo `notificationType=ALERT_ACTIVATED` |
| **Canal Android** | `alerts_default` (existente) |
| **Razón de no-cobertura previa** | Cubierto desde el principio. |

**Cambio respecto al estado actual**: el listener pasa de `features/push/.../AlertActivationPushListener.kt` (legacy) a `features/notification/.../AlertActivatedFcmListener.kt` (nuevo módulo hexagonal). Misma lógica, mismo filtro de severidad, mismas métricas. Strangler Fig: el listener viejo se elimina al final del despliegue de Fase 3 una vez verificada la equivalencia.

#### A.1.2 Listeners NO push (mantener intactos)

| Listener | Comportamiento | Acción Fase 2/3 |
|---|---|---|
| `TenantStatusBroadcastListener.kt:63` | WS broadcast snapshot tenant en ambas transiciones | Sin cambios. |
| `AlertStateChangedWebSocketListener.kt:30` | WS topic `/topic/tenant/{id}/alerts` con `AlertTransition` | Sin cambios. |
| `AlertStateChangedMqttEchoListener.kt:55` | MQTT echo a `GREENHOUSE/RESPONSE` | Sin cambios. |

### A.2 Nuevas notificaciones obligatorias

#### A.2.1 `ALERT_RESOLVED` ✅ obligatoria

| Campo | Valor |
|---|---|
| **Trigger** | `AlertStateChangedEvent` con `change.toResolved == true` |
| **Audiencia** | Misma lógica que `ALERT_ACTIVATED`: todos los tokens del tenant con preferencia `categoryAlerts` activa y severidad ≥ `minSeverity` y no en quiet hours. **Justificación**: el operario que recibió el push de activación necesita el cierre simétrico. Filtrar por "solo quien recibió el push de activación" requiere mirar la tabla de log F y añade complejidad sin ganancia clara. |
| **Severidad / urgencia** | Reaprovecha `alert.severityId`. Mismo `notify_push` flag — si una severidad está silenciada, ni activación ni resolución envían push. |
| **Título** | `notification.alert.resolved.title` con `{code}` → ej. `"Alerta resuelta: ALT-00010"` (es-ES) / `"Alert resolved: ALT-00010"` (en-US) |
| **Cuerpo** | `notification.alert.resolved.body` con placeholders `{message}`, `{actorDescription}`. Lógica de actor: extraído de `change.actor` (sealed `AlertActor.User/Device/System`, `AlertStateChange.kt:13`, ya populado por los use cases en `ResolveAlertUseCaseImpl.kt:45-49,83-87` y `ApplyAlertMqttSignalUseCaseImpl.kt:93`). |
| **`actorDescription`** | `User`: `"resuelta por {actor.displayName ?: actor.username ?: 'usuario #'+userId}"` — necesita lookup a `metadata.users` para resolver username. <br>`Device`: `"resuelta automáticamente vía MQTT"` <br>`System`: `"resuelta automáticamente"` |
| **Payload `data`** | Igual que `ALERT_ACTIVATED` + `notificationType=ALERT_RESOLVED` + `actorKind=USER|DEVICE|SYSTEM` + `actorUserId` (opcional) |
| **Canal Android** | `alerts_resolved` (canal **nuevo**, importance MEDIUM en lugar de HIGH — la resolución es positiva, no debe vibrar/sonar como CRITICAL). El cliente Android debe declarar el channel antes de la primera notificación. |
| **Razón de no-cobertura previa** | Listener `AlertActivationPushListener.kt:57` filtra explícitamente las resoluciones (`if (change.toResolved) return`). |

#### A.2.2 `ALERT_AGING` ✅ obligatoria

| Campo | Valor |
|---|---|
| **Trigger** | `AlertAgingDetectedEvent(alert: Alert, ageThresholdMinutes: Int)` (nuevo evento). Emitido por `AlertAgingDetector` (nuevo `@Scheduled`). |
| **Audiencia** | Misma que `ALERT_ACTIVATED` (tokens del tenant con preferencia activa y severidad ≥ min). |
| **Severidad / urgencia** | La de la alerta original. |
| **Título** | `notification.alert.aging.title` con `{code}`, `{ageHours}` → ej. `"Alerta sin resolver: ALT-00010 (45 min)"` |
| **Cuerpo** | `notification.alert.aging.body` con `{message}`, `{ageDescription}` |
| **Payload `data`** | `alertId`, `alertCode`, `severity`, `severityLevel`, `activatedAt` (timestamp original), `ageMinutes`, `notificationType=ALERT_AGING` |
| **Canal Android** | `alerts_aging` (canal **nuevo**, importance HIGH como la activación). |
| **Razón de no-cobertura previa** | No existe scheduler de detección ni evento publicado (auditoría 1.4.b). |

**Política del scheduler `AlertAgingDetector`**:
- `@Scheduled(fixedDelayString = "\${notification.aging.scan-interval:PT5M}")` — escanea cada 5 minutos por defecto, configurable.
- Query: `SELECT alert_id, severity_level, MAX(activated_at) AS last_activation FROM metadata.alerts WHERE is_resolved=false AND created_at < now() - threshold(severity)`. La activación más reciente se obtiene de `alert_state_changes` con `to_resolved=false` para evitar contar el "activated_at" original cuando la alerta togglea.
- Threshold por severidad (configurable vía property `notification.aging.thresholds`):
  - `CRITICAL` → 30 minutos.
  - `ERROR` → 2 horas.
  - `WARNING` → 8 horas.
  - `INFO` → no se notifica aging (filtrar antes de query).
- **Idempotencia**: tabla auxiliar `alert_aging_notifications_sent(alert_id, threshold_minutes_at_emission)` o, para evitar tabla extra, query contra el log F: `SELECT 1 FROM notification_log WHERE notification_type='ALERT_AGING' AND data->>'alertId' = ? AND status='SENT' AND sent_at > activated_at`. **Decisión**: usar el log F (sección F existe ya en MVP, evita tabla auxiliar). Si la alerta togglea (resolución intermedia), el scan posterior verá `last_activation` reciente y volverá a entrar en cuenta atrás → siguiente aging emite de nuevo. Comportamiento deseado: cada "ráfaga" de actividad se notifica como evento independiente.

### A.3 Eventos existentes del repo NO candidatos (con justificación)

Cubre la sección 1.4 de la auditoría. Estos eventos viajan por el sistema pero NO se proponen para push:

| Evento | Por qué NO es candidato |
|---|---|
| `DeviceCurrentValuesFlushedEvent` (`DeviceCurrentValuesFlushedEvent.kt:18-20`) | Cadencia ≤ 1/seg/tenant, ráfaga continua. Push generaría spam masivo. WS broadcast es la respuesta correcta y ya está. |
| `TenantStatusChangedEvent` (`TenantStatusChangedEvent.kt:16-28`) — fuente `GREENHOUSE_CRUD/SECTOR_CRUD/DEVICE_CRUD/SETTING_CRUD/USER_CRUD` | Granularidad CRUD admin. No aporta valor al operario móvil. WS para refrescar dashboards admin es suficiente. |
| `TenantStatusChangedEvent` con `Source = ALERT` | El listener real ya es `AlertStateChangedEvent`, este source es un alias. No duplicar. |

**Casos del prompt SIN evento publicado en código** (auditoría 1.4.b confirmó "no encontrado"):
- Dispositivos offline. Para implementar push: requiere primero modelar heartbeat/timeout en `metadata.devices` o nueva tabla. **Fuera de alcance Fase 2**.
- Gateways caídos. Requiere modelar `gateways` (no existen en BD). **Fuera de alcance**.
- Sensores sin lecturas en X tiempo. Requiere detector basado en `last_seen_at` de `device_current_values`. **Fuera de alcance** (similitud con ALERT_AGING; si en Fase 4 se quiere, se reusa el patrón).
- Suscripciones que caducan. Requiere modelar `subscriptions`. **Fuera de alcance**.
- Resolución automática por timeout. Sería un caso particular de aging, ya cubierto al notificar el aging.

---

## B — Modelo de preferencias de usuario

### B.1 Tabla `metadata.user_notification_preferences`

```sql
-- Migración V41
CREATE TABLE metadata.user_notification_preferences (
    user_id              BIGINT      PRIMARY KEY REFERENCES metadata.users(id) ON DELETE CASCADE,

    -- Categorías (extensibles vía nuevas columnas BOOLEAN)
    category_alerts      BOOLEAN     NOT NULL DEFAULT TRUE,
    category_devices     BOOLEAN     NOT NULL DEFAULT TRUE,
    category_subscription BOOLEAN    NOT NULL DEFAULT TRUE,

    -- Severidad mínima (filtra ALERT_ACTIVATED, ALERT_RESOLVED, ALERT_AGING)
    -- Reusa AlertSeverity.level: 1=INFO, 2=WARNING, 3=ERROR, 4=CRITICAL
    min_alert_severity   SMALLINT    NOT NULL DEFAULT 1
                         CHECK (min_alert_severity BETWEEN 1 AND 4),

    -- Quiet hours en TZ del usuario. NULL = sin quiet hours.
    quiet_hours_start    TIME        NULL,
    quiet_hours_end      TIME        NULL,
    quiet_hours_timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/Madrid',

    -- Canal preferido (futuro). Hoy solo PUSH se entrega; el resto reservado.
    preferred_channel    VARCHAR(16) NOT NULL DEFAULT 'PUSH'
                         CHECK (preferred_channel IN ('PUSH','EMAIL','SMS','WHATSAPP')),

    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Backfill: una fila por usuario existente con defaults razonables (todo activado, sin quiet hours)
INSERT INTO metadata.user_notification_preferences (user_id)
SELECT id FROM metadata.users
ON CONFLICT (user_id) DO NOTHING;

CREATE TRIGGER trg_user_notif_prefs_updated_at
    BEFORE UPDATE ON metadata.user_notification_preferences
    FOR EACH ROW EXECUTE FUNCTION metadata.update_timestamp();
```

**Nota**: la función `metadata.update_timestamp()` ya existe en migraciones anteriores (verificar V31 seed). Si no existe, se incluye en V41.

### B.2 Lógica de filtrado

Pseudocódigo del filtrado por usuario, ejecutado en el orquestador `DispatchNotificationUseCase`:

```
para cada token del tenant:
    user = lookup user(token.userId)
    prefs = preferences(user.id)

    si !prefs.categoryAlerts: log DROPPED_BY_PREFERENCE; siguiente
    si alert.severityLevel < prefs.minAlertSeverity: log DROPPED_BY_PREFERENCE; siguiente
    si en quiet_hours(prefs, now()): log DROPPED_BY_QUIET_HOURS; siguiente
    si dedup_recent(notificationType, alertCode, user.id): log DROPPED_BY_DEDUP; siguiente
    enqueue token para envío FCM
```

**Quiet hours wrap-around**: si `quiet_hours_start=22:00` y `quiet_hours_end=07:00`, la ventana cruza medianoche. Cálculo con timezone del usuario (`prefs.quietHoursTimezone`). Si `start == end`, no hay quiet hours (ventana cero).

### B.3 Endpoints REST

```
GET  /api/v1/users/me/notification-preferences
PUT  /api/v1/users/me/notification-preferences
```

DTO:
```kotlin
data class UserNotificationPreferencesResponse(
    val categoryAlerts: Boolean,
    val categoryDevices: Boolean,
    val categorySubscription: Boolean,
    val minAlertSeverity: Int,           // 1..4
    val quietHoursStart: String?,         // "22:00"
    val quietHoursEnd: String?,           // "07:00"
    val quietHoursTimezone: String,       // "Europe/Madrid"
    val preferredChannel: String          // "PUSH"
)
```

JWT obligatorio. `userId` resuelto desde `Authentication.name` siguiendo el patrón de `PushTokenController.kt:42`.

---

## C — Internacionalización del payload

### C.1 Locale del usuario

```sql
-- Migración V42
ALTER TABLE metadata.users
    ADD COLUMN locale VARCHAR(8) NOT NULL DEFAULT 'es-ES'
    CHECK (locale ~ '^[a-z]{2}-[A-Z]{2}$');
```

Default `es-ES` para preservar comportamiento actual de los usuarios existentes.

### C.2 MessageSource Spring

`@Bean` `ResourceBundleMessageSource` configurado en nuevo `NotificationI18nConfig.kt`:

```
Bundles:
  src/main/resources/i18n/notifications_es.properties
  src/main/resources/i18n/notifications_en.properties
```

Claves mínimas:
```
notification.alert.activated.title        # "Nueva alerta: {0}"
notification.alert.resolved.title         # "Alerta resuelta: {0}"
notification.alert.resolved.body          # "{0} — {1}"
notification.alert.aging.title            # "Alerta sin resolver: {0} ({1} min)"
notification.alert.aging.body             # "{0} — sin atender desde hace {1}"
notification.actor.user                   # "resuelta por {0}"
notification.actor.device                 # "resuelta automáticamente vía sensor"
notification.actor.system                 # "resuelta automáticamente"
notification.severity.INFO                # "Información"
notification.severity.WARNING             # "Aviso"
notification.severity.ERROR               # "Error"
notification.severity.CRITICAL            # "Crítico"
```

### C.3 Estrategia de resolución

- El payload se renderiza por **destinatario**: cada token → user → `user.locale` → `Locale.forLanguageTag(locale)`.
- El body que viene del operador (`alert.message`, etc.) NO se traduce — es texto libre. Se pre-anteponen literales traducidos cuando aplica (ej. el actor en `ALERT_RESOLVED`).
- Severidades: el `severity.name` (`CRITICAL`, etc.) se traduce vía `notification.severity.{name}`. Si la clave no existe en el bundle, fallback al name original.
- Fallback: si el bundle del locale del usuario no tiene una clave, Spring `MessageSource` cae a `notifications.properties` (default) → `notifications_es.properties`.

### C.4 Endpoint para cambiar locale

```
PUT /api/v1/users/me/locale
Body: { "locale": "en-US" }
```

**Decisión**: integrar como parte del DTO de preferencias (PUT a `/me/notification-preferences`) en lugar de endpoint separado. Reduce superficie API.

---

## D — Deduplicación / rate limiting

### D.1 Política

- Por tupla `(notificationType, alertCode, userId)`: si ya se ha enviado un push de ese tipo a ese usuario para esa alerta en los últimos `dedupWindowSeconds`, se descarta y se loggea `DROPPED_BY_DEDUP`.
- **Default por tipo**:
  - `ALERT_ACTIVATED`: 60 segundos.
  - `ALERT_RESOLVED`: 60 segundos.
  - `ALERT_AGING`: igual al threshold de la severidad (no re-notificar el mismo aging dentro de la ventana del threshold).
- Configurable vía properties: `notification.dedup.window.alert-activated`, etc.

### D.2 Implementación

Cache en Redis usando el `RedisTemplate<String, Any>` ya configurado (`config/RedisDataSource.kt:71`).

Patrón: `SET notif:dedup:{notificationType}:{alertCode}:{userId} 1 NX EX <window>`. Si el SET devuelve `null` (clave ya existía), se descarta.

Uso del mismo patrón que `SensorDeduplicationService.kt:34` (precedente del repo).

Key prefix: `notif:dedup:` (no choca con el prefix de cache existente `ts-app::`).

### D.3 Fallback si Redis no responde

`@Cacheable` no aplica aquí porque queremos failure-mode "fail open" (mejor enviar duplicados que perder un push). Si Redis falla:
- Captura excepción.
- Loggea `WARN`.
- Incremento métrica `notification.dedup.redis.failures`.
- **Continuar** con el envío (fail open).

---

## E — Robustez del envío

### E.1 Envío asíncrono

- Nuevo executor `fcmSendExecutor` en `config/NotificationAsyncConfig.kt`, paralelo al ya existente `wsBroadcastExecutor` (`WebSocketBroadcastAsyncConfig.kt:30`).
- Pool: `core=2, max=8, queue=200, rejection=CallerRunsPolicy` (defensivo: si la cola se llena, el thread del listener absorbe en lugar de descartar).
- El listener FCM (`AlertActivatedFcmListener`, etc.) llama a un servicio `@Async("fcmSendExecutor")` para que la transacción del listener cierre rápido y el envío FCM no bloquee el thread del bus de eventos.

### E.2 Retry exponencial

- Spring Retry: `implementation("org.springframework.retry:spring-retry")` + `implementation("org.springframework:spring-aspects")` (no están en el `build.gradle.kts` actual; se añaden).
- `@Retryable(value = [FirebaseMessagingException::class], maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 5000))` en `FcmSenderService.send(...)`.
- Solo reintentar errores **transitorios** FCM:
  - `INTERNAL`, `UNAVAILABLE`, `QUOTA_EXCEEDED`, `THIRD_PARTY_AUTH_ERROR` (lista exacta vetada por código retornado).
- NO reintentar `UNREGISTERED`, `INVALID_ARGUMENT`, `SENDER_ID_MISMATCH`: son permanentes; ya disparan delete del token.
- `@Recover` final: marca `FAILED` en el log F.

### E.3 Métricas Micrometer

| Métrica | Tipo | Tags |
|---|---|---|
| `notification.dispatched` (renombre de `push.fcm.sent`) | Counter | `notificationType`, `result=SUCCESS\|FAILURE\|DROPPED_*` |
| `notification.fcm.duration` (nueva) | Timer | `notificationType` |
| `notification.fcm.failed` (existente, mantener) | Counter | `reason=UNREGISTERED\|INVALID_ARGUMENT\|EXCEPTION\|...` |
| `notification.fcm.token.invalidated` (nueva) | Counter | `reason=UNREGISTERED\|INVALID_ARGUMENT` |
| `notification.dedup.dropped` | Counter | `notificationType` |
| `notification.preference.dropped` | Counter | `notificationType`, `reason=CATEGORY\|MIN_SEVERITY\|QUIET_HOURS` |
| `notification.aging.scan.duration` | Timer | — |
| `notification.aging.detected` | Counter | `severity` |

Las métricas existentes `push.fcm.sent` y `push.fcm.failed` se mantienen como aliases durante el periodo Strangler Fig (deprecation log al arrancar).

### E.4 Logs estructurados

Patrón con MDC: `tenantId`, `userId`, `notificationType`, `alertCode`, `fcmMessageId`. Reusa el patrón ya existente del repo.

### E.5 Invalidación automática de tokens

Mantener exactamente la lógica de `FcmPushService.kt:76-82`. Mover al nuevo `FcmSenderService` del módulo `notification` sin cambios funcionales.

---

## F — Tabla de log de notificaciones enviadas

### F.1 Migración

```sql
-- Migración V43
CREATE TABLE metadata.notification_log (
    id                  BIGINT       PRIMARY KEY DEFAULT metadata.generate_tsid(),
    tenant_id           BIGINT       NOT NULL REFERENCES metadata.tenants(id) ON DELETE CASCADE,
    user_id             BIGINT       NOT NULL REFERENCES metadata.users(id)   ON DELETE CASCADE,
    device_token_id     BIGINT       NULL REFERENCES metadata.push_tokens(id) ON DELETE SET NULL,
    notification_type   VARCHAR(32)  NOT NULL,
    -- payload completo en JSON (title, body, data, channel)
    payload_json        JSONB        NOT NULL,
    status              VARCHAR(32)  NOT NULL CHECK (status IN (
        'PENDING','SENT','FAILED','DROPPED_BY_PREFERENCE',
        'DROPPED_BY_QUIET_HOURS','DROPPED_BY_DEDUP','TOKEN_INVALIDATED'
    )),
    fcm_message_id      VARCHAR(128) NULL,
    error               TEXT         NULL,
    sent_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_log_user_at ON metadata.notification_log(user_id, sent_at DESC);
CREATE INDEX idx_notification_log_tenant_at ON metadata.notification_log(tenant_id, sent_at DESC);
CREATE INDEX idx_notification_log_type_at ON metadata.notification_log(notification_type, sent_at DESC);
CREATE INDEX idx_notification_log_status ON metadata.notification_log(status) WHERE status != 'SENT';

COMMENT ON TABLE metadata.notification_log IS
    'Audit trail append-only de cada intento de envío de notificación. Paralelo a metadata.alert_state_changes para el dominio de notificaciones.';
```

### F.2 Política de retención

Retention: 90 días por defecto. Configurable vía property `notification.log.retention-days`. Job `@Scheduled` (cron diario nocturno):
```sql
DELETE FROM metadata.notification_log WHERE sent_at < now() - INTERVAL ':retention days';
```

(NO TimescaleDB hypertable; no se justifica volumen previsto).

### F.3 Writer

Cada intento de envío persiste **una fila** con su status final (no se actualiza). Si el envío luego falla en retry, se persiste **otra fila** con `FAILED`. Append-only puro, igual que `alert_state_changes`.

Excepción: el status `PENDING` no se persiste en este modelo (no hay cola persistente, todo es síncrono dentro del executor). Solo se escribe al final con el status terminal.

---

## G — Endpoints REST nuevos

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/users/me` | Devuelve perfil con `locale` (para que el cliente sepa qué locale tiene). Si ya existe, ampliar. |
| `PATCH` | `/api/v1/users/me` | Permite actualizar `locale`. |
| `GET` | `/api/v1/users/me/notification-preferences` | Lee preferencias (crea defaults si no existen). |
| `PUT` | `/api/v1/users/me/notification-preferences` | Actualiza preferencias completas. |
| `GET` | `/api/v1/users/me/notifications?cursor=<tsid>&limit=50` | Log paginado de notificaciones recibidas (cursor por TSID descendente, máximo 50 por página). |
| **(existentes, mantener)** `POST /api/v1/push-tokens` y `DELETE /api/v1/push-tokens/{token}` | — | `PushTokenController.kt:31-58`. Sin cambios. |

**Decisión**: NO se añade `POST /api/v1/devices/register` ni `DELETE /api/v1/devices/{id}` que sugería el prompt original — los endpoints de tokens en `/api/v1/push-tokens/*` ya cumplen ese rol y romper esa API tendría coste de migración del cliente sin ganancia.

---

## H — Tests

Listado (no código). Cada bullet = un test que la implementación debe pasar.

### H.1 Unit tests (MockK, sin Spring)

- `DispatchNotificationUseCase.shouldSkipWhenCategoryDisabled`
- `DispatchNotificationUseCase.shouldSkipWhenSeverityBelowThreshold`
- `DispatchNotificationUseCase.shouldSkipWhenInQuietHoursWrappingMidnight`
- `DispatchNotificationUseCase.shouldSkipWhenInQuietHoursSameDay`
- `DispatchNotificationUseCase.shouldDispatchToAllElegibleTokens`
- `DispatchNotificationUseCase.shouldFailOpenWhenRedisDedupErrors`
- `AlertResolvedFcmListener.shouldOnlyFireOnToResolvedTrue`
- `AlertActivatedFcmListener.shouldOnlyFireOnToResolvedFalse`
- `AlertActivatedFcmListener.shouldRespectSeverityNotifyPushFlag`
- `AlertAgingDetector.shouldNotEmitIfAlreadyNotifiedWithinThreshold`
- `AlertAgingDetector.shouldEmitForCriticalAfter30Min`
- `AlertAgingDetector.shouldNotEmitForInfoEverySeverityFiltered`
- `NotificationI18n.shouldRenderEsByDefault`
- `NotificationI18n.shouldRenderEnWhenUserLocaleEnUS`
- `NotificationI18n.shouldFallbackToDefaultBundleWhenLocaleMissing`
- `NotificationDedupService.shouldDropDuplicateInWindow`
- `NotificationDedupService.shouldAllowAfterWindowExpired`
- `FcmSenderService.shouldRetry3TimesOnInternalError`
- `FcmSenderService.shouldNotRetryOnUnregistered`
- `FcmSenderService.shouldDeleteTokenOnUnregistered`
- `FcmSenderService.shouldDeleteTokenOnInvalidArgument`

### H.2 Integration tests (`@SpringBootTest`, BD real con Testcontainers)

- `Resolve alert via API → push de resolución llega con título 'Alerta resuelta: <code>' y actor 'resuelta por <username>'`.
- `MQTT signal cierra alerta → push de resolución llega con actor 'resuelta automáticamente vía sensor'`.
- `Activación + dedup window → solo un push por (alertCode, userId) en 60s`.
- `Aging scheduler con CRITICAL > 30min → push ALERT_AGING al user con preferencias activas`.
- `Aging scheduler ya notificado dentro de threshold → no re-emite (idempotencia vía notification_log)`.
- `User con minAlertSeverity=ERROR → no recibe push de WARNING; SI recibe push de CRITICAL`.
- `User en quiet_hours 22-07 Europe/Madrid → push se loggea DROPPED_BY_QUIET_HOURS`.
- `Token UNREGISTERED → fila notification_log con status TOKEN_INVALIDATED + fila push_tokens borrada`.
- `Token sigue válido para mismo user en otro dispositivo → siguiente envío llega solo al válido`.
- `Locale en-US del user → payload viene en inglés (severity 'CRITICAL' → 'Critical')`.
- `Endpoint GET /api/v1/users/me/notifications devuelve solo del usuario autenticado, paginado por cursor TSID`.

### H.3 ArchUnit tests

Replicar exactamente las reglas del módulo `alert` aplicadas a `notification`:

- `notificationDomainMustNotDependOnSpring`
- `notificationDomainMustNotDependOnInfrastructure`
- `notificationDomainMustNotDependOnDto`
- `notificationUseCaseImplsMustImplementInputPort`
- `notificationQueryAdaptersMustImplementOutputPort` (si aplica)

### H.4 Regression tests

- El test ya existente para activación de alerta (si lo hay en `src/test`) sigue pasando sin modificación. Si el listener viejo `AlertActivationPushListener` pasa a deprecated, mantener su test verde hasta su eliminación.

---

## I — Estructura del nuevo módulo `notification`

Estructura propuesta (replica del patrón de `features/alert/`):

```
features/notification/
├── domain/
│   ├── model/
│   │   ├── NotificationType.kt              # enum: ALERT_ACTIVATED, ALERT_RESOLVED, ALERT_AGING
│   │   ├── NotificationStatus.kt            # enum: PENDING, SENT, FAILED, DROPPED_*, TOKEN_INVALIDATED
│   │   ├── NotificationCategory.kt          # enum: ALERTS, DEVICES, SUBSCRIPTION
│   │   ├── UserNotificationPreferences.kt   # data class, valor objeto, value class para ids
│   │   ├── QuietHours.kt                    # value class con lógica de wrap-around
│   │   ├── NotificationLogEntry.kt          # registro para append-only log
│   │   └── DispatchDecision.kt              # sealed: Send | Drop(reason)
│   ├── port/
│   │   ├── input/
│   │   │   ├── DispatchNotificationUseCase.kt
│   │   │   ├── GetUserPreferencesUseCase.kt
│   │   │   ├── UpdateUserPreferencesUseCase.kt
│   │   │   ├── ListUserNotificationsUseCase.kt
│   │   │   └── DetectAgingAlertsUseCase.kt
│   │   └── output/
│   │       ├── UserPreferencesRepositoryPort.kt
│   │       ├── NotificationLogRepositoryPort.kt
│   │       ├── NotificationDedupPort.kt        # Redis behind
│   │       ├── FcmSenderPort.kt                # FcmPushService cumple, vía adapter
│   │       ├── PushTokenLookupPort.kt          # delega en push.PushTokenRepository
│   │       ├── UserLookupPort.kt               # delega en user.UserRepository
│   │       ├── AlertSeverityLookupPort.kt      # delega en catalog.AlertSeverityRepository
│   │       └── AgingAlertScannerPort.kt        # query JPQL/SQL contra alerts
│   └── error/
│       └── NotificationError.kt             # sealed interface
├── application/
│   └── usecase/
│       ├── DispatchNotificationUseCaseImpl.kt
│       ├── GetUserPreferencesUseCaseImpl.kt
│       ├── UpdateUserPreferencesUseCaseImpl.kt
│       ├── ListUserNotificationsUseCaseImpl.kt
│       └── DetectAgingAlertsUseCaseImpl.kt
├── infrastructure/
│   ├── adapter/
│   │   ├── input/
│   │   │   ├── UserNotificationPreferencesController.kt
│   │   │   ├── UserNotificationLogController.kt
│   │   │   ├── AlertActivatedFcmListener.kt          # @TransactionalEventListener
│   │   │   ├── AlertResolvedFcmListener.kt           # @TransactionalEventListener
│   │   │   ├── AlertAgingFcmListener.kt              # @TransactionalEventListener
│   │   │   └── AlertAgingDetectorScheduler.kt        # @Scheduled, llama DetectAgingAlertsUseCase
│   │   └── output/
│   │       ├── UserPreferencesPersistenceAdapter.kt
│   │       ├── NotificationLogPersistenceAdapter.kt
│   │       ├── RedisNotificationDedupAdapter.kt
│   │       ├── FcmSenderAdapter.kt                   # delega en FcmPushService legacy
│   │       ├── PushTokenLookupAdapter.kt
│   │       ├── UserLookupAdapter.kt
│   │       ├── AlertSeverityLookupAdapter.kt
│   │       └── AgingAlertScannerAdapter.kt
│   └── config/
│       ├── NotificationModuleConfig.kt              # bean wiring
│       ├── NotificationAsyncConfig.kt               # fcmSendExecutor
│       ├── NotificationI18nConfig.kt                # MessageSource + bundles
│       ├── NotificationProperties.kt                # @ConfigurationProperties
│       └── NotificationRetryConfig.kt               # @EnableRetry
├── dto/
│   ├── request/
│   │   └── UpdateUserNotificationPreferencesRequest.kt
│   ├── response/
│   │   ├── UserNotificationPreferencesResponse.kt
│   │   └── UserNotificationLogEntryResponse.kt
│   └── mapper/
│       └── NotificationMappers.kt
└── jpa/
    ├── UserNotificationPreferencesEntity.kt
    ├── UserNotificationPreferencesRepository.kt
    ├── NotificationLogEntity.kt
    └── NotificationLogRepository.kt
```

### I.1 Posición de `features/push/` legacy

- Se conservan: `PushToken`, `PushTokenRepository`, `PushTokenService`, `PushTokenController`, `FcmPushService`, `FirebaseConfig`.
- `AlertActivationPushListener.kt` se **elimina al final del despliegue** una vez verificado en métricas y logs que `AlertActivatedFcmListener` (nuevo) emite el push correctamente. Strangler Fig.
- `FcmPushService.sendAlertToTenant` se mantiene como API pública del módulo legacy. El nuevo `FcmSenderAdapter` lo invoca. Al final de la migración Strangler Fig, su lógica se integra en `FcmSenderService` del nuevo módulo y la clase legacy se reduce a un re-export delegado o se elimina.

---

## J — Migraciones Flyway

| Versión | Fichero | Contenido |
|---|---|---|
| `V41` | `V41__create_user_notification_preferences.sql` | Tabla `metadata.user_notification_preferences` + backfill por usuario existente + trigger updated_at. |
| `V42` | `V42__add_locale_to_users.sql` | `ALTER TABLE metadata.users ADD COLUMN locale VARCHAR(8) NOT NULL DEFAULT 'es-ES'`. |
| `V43` | `V43__create_notification_log.sql` | Tabla `metadata.notification_log` + 4 índices + comentario. |

Numeración secuencial respecto a `V40` actual. Sin saltos. **Idempotencia**: todas las migraciones usan `IF NOT EXISTS` donde aplica, siguiendo el patrón de `V38`.

---

## K — Cambios fuera del repo (K8s / config)

### K.1 ConfigMap dev y prod

Añadir al `application.yaml` montado vía configmap (`/home/admin/companies/apptolast/invernaderos/k8s/11-api-dev/02-configmap.yaml` y `.../10-api-prod/02-configmap.yaml`):

```yaml
notification:
  aging:
    scan-interval: PT5M           # Duration ISO 8601
    thresholds:
      CRITICAL: PT30M
      ERROR:    PT2H
      WARNING:  PT8H
      # INFO no se notifica
  dedup:
    enabled: true
    window:
      alert-activated: 60s
      alert-resolved:  60s
      alert-aging:     PT30M       # configurable; coincide con threshold mínimo
  log:
    retention-days: 90
  fcm:
    retry:
      max-attempts: 3
      initial-delay: 500ms
      multiplier: 2.0
      max-delay: 5s
```

### K.2 Secrets

No se necesitan secrets nuevos. El `firebase-admin-sa` ya existe y está montado correctamente en dev y prod.

### K.3 Deployment

No requiere cambios estructurales. La imagen API se reemplaza con la nueva versión que incluye el módulo `notification`. Sin cambios en mounts, env vars adicionales, ports, ni resources.

---

## L — Plan de despliegue por fases (Fase 3)

### L.1 Compatibilidad con cliente móvil

Ningún cambio rompe compatibilidad con el cliente móvil actual:

| Cambio backend | Impacto cliente |
|---|---|
| Nuevo canal Android `alerts_resolved` | El cliente debe declarar el channel para que el sistema lo muestre con la importance MEDIUM esperada. **Si no lo declara**, Android lo crea automáticamente con importance default (alto), aceptable como degradación. **Coordinación con cliente móvil recomendada pero no bloqueante**. |
| Nuevo canal Android `alerts_aging` | Mismo razonamiento. |
| Nuevo `data.notificationType` en payload | El cliente lo ignora hoy, pero **no rompe** el deep-link existente (sigue funcionando con `data.alertId`). Cuando el cliente lo soporte, podrá enrutar a una pantalla diferente para `ALERT_RESOLVED` vs `ALERT_ACTIVATED`. |
| Nuevo `data.actorKind`, `data.actorUserId` en `ALERT_RESOLVED` | Cliente lo ignora hoy. Cero impacto. |
| Endpoints `/api/v1/users/me/*` | El cliente puede empezar a usarlos cuando tenga UI; mientras tanto los defaults razonables (todo activado) replican el comportamiento actual. |
| `users.locale` default `es-ES` | Cero impacto hasta que el cliente envíe `PATCH /me` con otro locale. |

**Despliegue de la API es independiente del despliegue del cliente móvil**.

### L.2 Pasos de despliegue (a ejecutar en Fase 3, NO ahora)

Por orden:

1. **Migraciones Flyway**: V41, V42, V43 se aplican automáticamente al levantar el pod (Flyway hook ya configurado en `FlywayConfig.kt`).
2. **Backfill V41**: incluido en la migración (un INSERT con defaults).
3. **Despliegue dev** (rama `develop`):
   - Merge PR a `develop`.
   - El pipeline construye la imagen y empuja a `apptolast-invernadero-api-dev`.
   - Verificación en dev: smoke test de los 3 tipos de push (activación, resolución, aging) usando alertas reales.
4. **Despliegue prod** (rama `main`):
   - Una vez verificado dev durante 24-48h sin regresión.
   - Merge `develop → main`.
   - Pipeline despliega a `apptolast-invernadero-api-prod`.
5. **Limpieza Strangler Fig**: tras 1 semana en prod sin issues, eliminar `AlertActivationPushListener.kt` legacy. Migración cero-coste (sustituido por `AlertActivatedFcmListener`).

### L.3 Rollback

- Si hay regresión post-deploy:
  - Git revert del PR + redeploy.
  - Las migraciones V41/V42/V43 son aditivas y NO requieren rollback de schema (las tablas pueden quedar en BD sin uso).
  - El listener viejo `AlertActivationPushListener` sigue activo durante el periodo Strangler Fig, así que si el nuevo falla, los pushes de activación siguen llegando.

### L.4 Métricas a vigilar post-deploy

- `notification.dispatched{result=SUCCESS}` debe igualar el ritmo histórico de `push.fcm.sent` para `ALERT_ACTIVATED`.
- `notification.dispatched{result=FAILURE}` baja respecto al baseline (gracias al retry).
- `notification.aging.detected` aparece (no antes existía).
- Tabla `notification_log` se llena con cadencia esperada.
- Latencia P95 del envío FCM (`notification.fcm.duration`) baja del actual síncrono al async.

---

## M — Cierre Fase 2

Diseño completo. Decisiones tomadas con el usuario explícitamente:

| Tema | Resultado |
|---|---|
| Notificación de resolución de alerta | **Incluida (obligatoria)** |
| Notificación de aging | **Incluida (alcance)** |
| Tabla append-only de log | **Incluida en MVP** (V43 + endpoint GET /me/notifications) |
| Estructura | **Nuevo módulo hexagonal `features/notification/`** |
| Canales Android nuevos | `alerts_resolved`, `alerts_aging` (degradan amigablemente si el cliente no los declara) |
| Endpoints nuevos | `/api/v1/users/me/notification-preferences` (GET/PUT), `/api/v1/users/me/notifications` (GET paginado), `/api/v1/users/me` (GET/PATCH para locale) |
| Migraciones | V41, V42, V43 |
| Cambios K8s | Solo bloque `notification:` en configmap dev y prod. Sin secrets nuevos. |

**Esperando aprobación explícita del usuario para abrir Fase 3** (implementación + tests + despliegue dev `develop` y prod `main`). Hasta entonces:

- No se toca `src/main/kotlin/**`.
- No se añaden migraciones (`V41+`).
- No se modifican configmaps de K8s.
- No se hace merge a `develop` ni a `main`.

Cuando confirmes, la Fase 3 se ejecuta como serie de PRs pequeños siguiendo el orden:

1. PR-1: módulo `notification` esqueleto + ArchUnit + V41 (preferencias) + endpoints prefs + tests.
2. PR-2: V42 (locale) + i18n bundles + MessageSource + GET/PATCH /me.
3. PR-3: V43 (log) + writer + endpoint GET /me/notifications + tests.
4. PR-4: dedup Redis + listener `AlertActivatedFcmListener` (Strangler Fig en paralelo al viejo) + métricas + tests.
5. PR-5: listener `AlertResolvedFcmListener` + canal `alerts_resolved` + tests.
6. PR-6: scheduler `AlertAgingDetectorScheduler` + listener + canal `alerts_aging` + tests.
7. PR-7: configmap K8s dev + verificación smoke en dev.
8. PR-8: configmap K8s prod + merge `develop → main` + verificación smoke en prod.
9. PR-9: cleanup — eliminar `AlertActivationPushListener` legacy.

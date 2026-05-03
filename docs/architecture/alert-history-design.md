# Alert history (real) + statistics — Phase 2 design

> Decision document for the redesign that exposes the existing `metadata.alert_state_changes` audit log as a first-class read API and adds derived statistics. Phase 1 audit lives in conversation history; this is the actionable design.

## 1. Decisions

| Topic | Decision | Rationale |
|---|---|---|
| Histórico semantics | "Histórico" = stream of transitions, not list of resolved alerts | One row per state change. Same alert with 17 cycles → 34 transition rows or 17 episode rows. |
| Actor persistence | Add `actor_user_id`, `actor_kind`, `actor_ref` to `alert_state_changes` (V40) | Required to answer "who resolved this" historically. |
| Backfill | Best-effort: latest API resolve per alert maps to `alerts.resolved_by_user_id`; older transitions stay NULL honestly. MQTT rows → `actor_kind='DEVICE'`, `actor_ref=NULL`. | No invention of data we never persisted. |
| Legacy `AlertController` | Keep working but make legacy `resolve`/`reopen` delegate to the hexagonal use cases (which already write state_changes). Add `Deprecation: true` + `Sunset` headers. | Mobile keeps working untouched, history starts being captured for legacy paths immediately. |
| Hypertable | NO in this iteration. Keep `alert_state_changes` in `metadata` schema as regular Postgres table. | Volume is low (alerts vs sensor readings). Cross-schema (metadata vs iot) hypertable adds complexity. Re-evaluate when volume justifies it. |
| WebSocket broadcast | Add `AlertStateChangedWebSocketListener` → publish to `/topic/tenant/{tenantId}/alerts` with `AlertTransitionResponse` payload. | UI can react in real time without polling. |
| Statistics scope | Recurrence + MTTR + Timeseries + Active-duration + By-actor + Summary. | Covers the user's "all the stats we can squeeze out". |

## 2. Migration V40 — `actor` columns + backfill

```sql
-- V40__add_actor_to_alert_state_changes.sql

ALTER TABLE metadata.alert_state_changes
    ADD COLUMN actor_user_id BIGINT NULL REFERENCES metadata.users(id) ON DELETE SET NULL,
    ADD COLUMN actor_kind    VARCHAR(16) NOT NULL DEFAULT 'SYSTEM'
                CHECK (actor_kind IN ('USER', 'DEVICE', 'SYSTEM')),
    ADD COLUMN actor_ref     VARCHAR(128) NULL;

-- Best-effort backfill:
-- 1) For source='MQTT' rows → actor_kind='DEVICE', actor_ref stays NULL (we never persisted gateway).
UPDATE metadata.alert_state_changes
   SET actor_kind = 'DEVICE'
 WHERE source = 'MQTT';

-- 2) For source='SYSTEM' rows → actor_kind='SYSTEM' (already default; explicit for clarity).
UPDATE metadata.alert_state_changes
   SET actor_kind = 'SYSTEM'
 WHERE source = 'SYSTEM';

-- 3) For source='API' rows → mark as USER. Try to recover actor_user_id from
--    alerts.resolved_by_user_id, but ONLY for the latest 'to_resolved=true'
--    transition per alert (older API transitions are unrecoverable).
UPDATE metadata.alert_state_changes
   SET actor_kind = 'USER'
 WHERE source = 'API';

WITH latest_resolution_per_alert AS (
    SELECT DISTINCT ON (alert_id) id, alert_id
      FROM metadata.alert_state_changes
     WHERE source = 'API' AND to_resolved = TRUE
     ORDER BY alert_id, at DESC
)
UPDATE metadata.alert_state_changes asc_row
   SET actor_user_id = a.resolved_by_user_id
  FROM latest_resolution_per_alert latest
  JOIN metadata.alerts a ON a.id = latest.alert_id
 WHERE asc_row.id = latest.id
   AND a.resolved_by_user_id IS NOT NULL;

-- Indexes for actor lookups (e.g., "alerts resolved by user X")
CREATE INDEX idx_alert_state_changes_actor_user ON metadata.alert_state_changes(actor_user_id) WHERE actor_user_id IS NOT NULL;
CREATE INDEX idx_alert_state_changes_actor_kind ON metadata.alert_state_changes(actor_kind);

COMMENT ON COLUMN metadata.alert_state_changes.actor_user_id IS 'User who triggered this transition. Populated for source=API only.';
COMMENT ON COLUMN metadata.alert_state_changes.actor_kind IS 'USER (API), DEVICE (MQTT), SYSTEM (auto/scheduler).';
COMMENT ON COLUMN metadata.alert_state_changes.actor_ref IS 'Free-form actor reference: gateway/device id for DEVICE, job name for SYSTEM.';
```

## 3. Domain (pure Kotlin)

```kotlin
// features/alert/domain/model/AlertActor.kt
sealed interface AlertActor {
    data class User(val userId: Long, val username: String?, val displayName: String?) : AlertActor
    data class Device(val deviceRef: String?) : AlertActor
    data object System : AlertActor
}

// features/alert/domain/model/AlertTransition.kt — read-model enriquecido
data class AlertTransition(
    val transitionId: Long,
    val at: Instant,
    val fromResolved: Boolean,
    val toResolved: Boolean,
    val source: AlertSignalSource,
    val rawValue: String?,
    val actor: AlertActor,
    // alert context
    val alertId: Long,
    val alertCode: String,
    val alertMessage: String?,
    val alertTypeId: Short?,
    val alertTypeName: String?,
    val severityId: Short?,
    val severityName: String?,
    val severityLevel: Short?,
    val severityColor: String?,
    // physical context
    val sectorId: Long,
    val sectorCode: String?,
    val greenhouseId: Long?,
    val greenhouseName: String?,
    val tenantId: Long,
    // temporal correlation
    val previousTransitionAt: Instant?,
    val episodeStartedAt: Instant?,
    val episodeDurationSeconds: Long?,
    // accumulated counters
    val occurrenceNumber: Long,
    val totalTransitionsSoFar: Long,
)

// features/alert/domain/model/AlertEpisode.kt
data class AlertEpisode(
    val alertId: Long,
    val alertCode: String,
    val triggeredAt: Instant,
    val resolvedAt: Instant?,           // null if still open
    val durationSeconds: Long?,         // null if still open
    val triggerSource: AlertSignalSource,
    val resolveSource: AlertSignalSource?,
    val triggerActor: AlertActor,
    val resolveActor: AlertActor?,
    val severityId: Short?,
    val severityName: String?,
    val sectorId: Long,
    val sectorCode: String?,
)

// features/alert/domain/port/output/AlertHistoryQueryPort.kt
interface AlertHistoryQueryPort {
    fun findTransitionsByAlertId(alertId: Long, tenantId: TenantId, order: SortOrder): List<AlertTransition>
    fun findTransitions(query: AlertEventsQuery): PagedResult<AlertTransition>
    fun findEpisodes(query: AlertEpisodesQuery): PagedResult<AlertEpisode>
}

// features/alert/domain/port/output/AlertStatsQueryPort.kt
interface AlertStatsQueryPort {
    fun recurrence(query: RecurrenceStatsQuery): List<RecurrenceBucket>
    fun mttr(query: MttrStatsQuery): List<MttrBucket>
    fun timeseries(query: TimeseriesStatsQuery): List<TimeseriesBucket>
    fun activeDuration(query: ActiveDurationStatsQuery): List<ActiveDurationBucket>
    fun byActor(query: ByActorStatsQuery): List<ByActorBucket>
    fun summary(tenantId: TenantId, from: Instant, to: Instant): AlertStatsSummary
}
```

## 4. REST endpoints (all hexagonal, all under `/api/v1/tenants/{tenantId}/...`)

### Histórico (3)

| Verb | Path | Purpose |
|---|---|---|
| GET | `/alerts/{alertId}/history?order=ASC\|DESC` | Timeline of transitions of one alert |
| GET | `/alert-events?from=&to=&source=&severityId=&alertTypeId=&sectorId=&greenhouseId=&code=&actorUserId=&transitionKind=ANY\|OPEN\|CLOSE&page=&size=` | Paginated tenant-wide feed of transitions (this REPLACES the "Histórico" tab) |
| GET | `/alert-events/episodes?from=&to=&severityId=&sectorId=&code=&onlyClosed=&page=&size=` | Episodes (open→close pairs) for the tenant |

### Estadísticas (6)

| Verb | Path |
|---|---|
| GET | `/alerts/stats/recurrence?from=&to=&groupBy=code\|type\|severity\|sector\|greenhouse&limit=` |
| GET | `/alerts/stats/mttr?from=&to=&groupBy=severity\|type\|sector\|code` |
| GET | `/alerts/stats/timeseries?from=&to=&bucket=hour\|day\|week\|month&groupBy=severity\|type` |
| GET | `/alerts/stats/active-duration?from=&to=&groupBy=code\|sector` |
| GET | `/alerts/stats/by-actor?from=&to=&role=resolver\|opener` |
| GET | `/alerts/stats/summary?from=&to=` |

### Endpoint adicional

| Verb | Path | Purpose |
|---|---|---|
| GET | `/alerts/count/unresolved/sector/{sectorId}` | Existing service method, never exposed; expose now as hex equivalent |

## 5. DTOs (one file per DTO)

- `AlertTransitionResponse` (mirrors domain `AlertTransition` with primitives only)
- `AlertActorResponse` (sealed-like JSON with `kind: "USER"|"DEVICE"|"SYSTEM"` + `userId`/`username`/`displayName`/`ref` as nullable)
- `AlertEpisodeResponse`
- `PagedResponse<T>` (generic) — items + page/size/total/hasMore
- `RecurrenceBucketResponse`, `MttrBucketResponse`, `TimeseriesBucketResponse`, `ActiveDurationBucketResponse`, `ByActorBucketResponse`
- `AlertStatsSummaryResponse` (totalActiveNow, openedToday, closedToday, mttrTodaySeconds, top3RecurrentCodesThisWeek)

## 6. Native queries (illustrative — final SQL in adapter)

`findTransitions` core query uses window functions for `previousTransitionAt`, `episodeStartedAt`, `occurrenceNumber`, `totalTransitionsSoFar`:

```sql
SELECT
  asc.id, asc.at, asc.from_resolved, asc.to_resolved, asc.source, asc.raw_value,
  asc.actor_user_id, asc.actor_kind, asc.actor_ref,
  u.username, u.display_name,
  a.id AS alert_id, a.code, a.message, a.alert_type_id, a.severity_id, a.sector_id, a.tenant_id,
  at.name AS alert_type_name,
  sev.name AS severity_name, sev.level AS severity_level, sev.color AS severity_color,
  s.code AS sector_code, s.greenhouse_id,
  g.name AS greenhouse_name,
  LAG(asc.at) OVER (PARTITION BY asc.alert_id ORDER BY asc.at)                                AS previous_transition_at,
  CASE WHEN asc.to_resolved = TRUE THEN
    LAST_VALUE(CASE WHEN asc.to_resolved = FALSE THEN asc.at END)
      OVER (PARTITION BY asc.alert_id ORDER BY asc.at
            ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING)
  END                                                                                        AS episode_started_at,
  COUNT(*) FILTER (WHERE asc.to_resolved = FALSE)
    OVER (PARTITION BY asc.alert_id ORDER BY asc.at
          ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)                                  AS occurrence_number,
  ROW_NUMBER() OVER (PARTITION BY asc.alert_id ORDER BY asc.at)                              AS total_transitions_so_far
FROM metadata.alert_state_changes asc
JOIN metadata.alerts a ON a.id = asc.alert_id
LEFT JOIN metadata.users u ON u.id = asc.actor_user_id
LEFT JOIN metadata.alert_types at ON at.id = a.alert_type_id
LEFT JOIN metadata.alert_severities sev ON sev.id = a.severity_id
LEFT JOIN metadata.sectors s ON s.id = a.sector_id
LEFT JOIN metadata.greenhouses g ON g.id = s.greenhouse_id
WHERE a.tenant_id = :tenantId
  AND asc.at >= :from AND asc.at < :to
  -- + dynamic filters
ORDER BY asc.at DESC
LIMIT :size OFFSET :offset;
```

## 7. Legacy patch

`AlertController.resolve()` (line 346) and `reopen()` (line 375): refactor body to delegate to the hexagonal use cases (`ResolveAlertUseCaseImpl.resolve`/`reopen`). This:
- Preserves the legacy URL/contract (no client breakage).
- Makes every legacy resolve/reopen write a row to `alert_state_changes`.
- Triggers `AlertStateChangedEvent` (FCM push + future WS broadcast).

Add HTTP headers on all legacy endpoints:
```
Deprecation: true
Sunset: <90 days from deploy>
Link: <https://inverapi-prod.apptolast.com/swagger-ui.html#tenant-alerts>; rel="successor-version"
```

## 8. WebSocket broadcast

New listener `AlertStateChangedWebSocketListener.kt`:
- `@TransactionalEventListener(phase = AFTER_COMMIT)` on `AlertStateChangedEvent`
- Map event → `AlertTransitionResponse` (using the same projection as REST, but for a single row)
- `simpMessagingTemplate.convertAndSend("/topic/tenant/${event.alert.tenantId}/alerts", payload)`

WebSocket security: existing `StompJwtAuthInterceptor` already handles auth.

## 9. Tests

- **Unit**: each use case (mocked ports), each domain mapping function.
- **Integration** (`@SpringBootTest` + Testcontainers Postgres):
  1. Create alert → MQTT activates (raw=1) → MQTT resolves (raw=0) → API reopens → API resolves: `/alerts/{id}/history` returns 5 transitions, `/alert-events/episodes` returns 2 episodes (one fully closed, one open→close).
  2. 17 abrir/cerrar cycles → 34 transitions in history, 17 episodes in episodes endpoint.
  3. Resolve via legacy `PUT /api/v1/alerts/{id}/resolve` → state_change row appears with `source=API`, `actor_user_id=...`.
  4. Stats: `recurrence` returns alerts ordered by activation count; `mttr` matches manual computation.
  5. WebSocket: subscribe to `/topic/tenant/{id}/alerts`, fire transition, receive payload.
- **ArchUnit**: domain layer remains framework-free.
- **Regression**: every legacy endpoint still returns the same shape (snapshot tests).

## 10. Out of scope (explicit non-goals for this PR)

- Hypertable migration of `alert_state_changes` (deferred).
- Fixing nullables `alert_type_id`/`severity_id`/`message` on `alerts` (deferred — separate ticket once we confirm count of legacy NULLs).
- Removing legacy `AlertController` (deprecated now, retired in a later PR after mobile migrates).

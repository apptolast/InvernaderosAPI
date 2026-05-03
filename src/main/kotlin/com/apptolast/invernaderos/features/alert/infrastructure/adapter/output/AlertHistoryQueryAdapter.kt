package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertEpisode
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertTransition
import com.apptolast.invernaderos.features.alert.domain.model.TransitionKind
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEpisodesQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEventsQuery
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertHistoryQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.PagedResult
import com.apptolast.invernaderos.features.shared.domain.model.SortOrder
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

/**
 * Read-side adapter for the alert history queries.
 *
 * All queries use [JdbcTemplate] against the metadata datasource because:
 * 1. The per-alert timeline and the tenant-wide feed both use SQL window functions
 *    (LAG, LAST_VALUE, ROW_NUMBER) which Spring Data JPA cannot express through JPQL.
 * 2. Dynamic WHERE clauses (up to 8 optional filters) are far cleaner to build with
 *    a StringBuilder + param list than through JPA Criteria or QueryDSL.
 *
 * The [AlertHistoryJpaRepository] is only used for the cheap tenant-ownership check.
 *
 * Design note for the single-row WebSocket path:
 * Instead of calling [findTransitionsByAlertId] for one row on every state change event
 * (which pays the full window-function cost), [AlertStateChangedWebSocketListener]
 * constructs the [AlertTransition] directly from the event payload plus a one-row
 * users-table lookup. This adapter does not expose a "get one" method deliberately.
 */
@Component
class AlertHistoryQueryAdapter(
    @Qualifier("metadataJdbcTemplate") private val jdbc: JdbcTemplate,
    private val jpaRepository: AlertHistoryJpaRepository,
) : AlertHistoryQueryPort {

    // -------------------------------------------------------------------
    // findTransitionsByAlertId  — single-alert timeline
    // -------------------------------------------------------------------

    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun findTransitionsByAlertId(
        alertId: Long,
        tenantId: TenantId,
        order: SortOrder,
    ): List<AlertTransition> {
        // Ownership check: if no rows → either no such alert or it belongs to another tenant.
        if (jpaRepository.countByAlertIdAndTenantId(alertId, tenantId.value) == 0L) return emptyList()

        val orderDir = if (order == SortOrder.ASC) "ASC" else "DESC"
        val sql = transitionSql(
            extraWhere = "AND asc.alert_id = ?",
            orderDir = orderDir,
            limit = "ALL",
            offset = "0",
        )
        return jdbc.query(sql, ::mapTransition, tenantId.value, Timestamp.from(Instant.EPOCH), Timestamp.from(FAR_FUTURE), alertId)
    }

    // -------------------------------------------------------------------
    // findTransitions — paginated tenant-wide feed
    // -------------------------------------------------------------------

    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun findTransitions(query: AlertEventsQuery): PagedResult<AlertTransition> {
        val (whereClauses, params) = buildTransitionWhere(query)
        val countSql = countTransitionSql(whereClauses)
        val total = jdbc.queryForObject(countSql, Long::class.java, *params.toTypedArray()) ?: 0L

        val dataSql = transitionSql(
            extraWhere = whereClauses,
            orderDir = "DESC",
            limit = query.size.toString(),
            offset = (query.page * query.size).toString(),
        )
        val items = jdbc.query(dataSql, ::mapTransition, *params.toTypedArray())
        return PagedResult(
            items = items,
            page = query.page,
            size = query.size,
            total = total,
            hasMore = (query.page.toLong() + 1) * query.size < total,
        )
    }

    // -------------------------------------------------------------------
    // findEpisodes — open→close pairs
    // -------------------------------------------------------------------

    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun findEpisodes(query: AlertEpisodesQuery): PagedResult<AlertEpisode> {
        val (whereClauses, params) = buildEpisodesWhere(query)

        val countSql = """
            SELECT COUNT(*) FROM (
              $episodeCoreSql
              $whereClauses
            ) ep
        """.trimIndent()
        val total = jdbc.queryForObject(countSql, Long::class.java, *params.toTypedArray()) ?: 0L

        // SQL-safe: query.size is enforced by the use case to be in 1..200, query.page >= 0.
        // Use Long arithmetic to avoid Int overflow on very large page numbers.
        val safeOffset = (query.page.toLong() * query.size).toString()
        val dataSql = """
            $episodeCoreSql
            $whereClauses
            ORDER BY open_at DESC
            LIMIT ${query.size} OFFSET $safeOffset
        """.trimIndent()

        val items = jdbc.query(dataSql, ::mapEpisode, *params.toTypedArray())
        return PagedResult(
            items = items,
            page = query.page,
            size = query.size,
            total = total,
            hasMore = (query.page.toLong() + 1) * query.size < total,
        )
    }

    // -------------------------------------------------------------------
    // SQL helpers
    // -------------------------------------------------------------------

    /**
     * Core window-function query for alert transitions.
     * Parameters (in order): tenantId, fromInstant, toInstant, [extraWhere params...]
     */
    private fun transitionSql(
        extraWhere: String,
        orderDir: String,
        limit: String,
        offset: String,
    ): String = """
        SELECT
          asc.id                         AS transition_id,
          asc.at,
          asc.from_resolved,
          asc.to_resolved,
          asc.source,
          asc.raw_value,
          asc.actor_kind,
          asc.actor_user_id,
          asc.actor_ref,
          u.username,
          u.display_name,
          a.id                           AS alert_id,
          a.code                         AS alert_code,
          a.message                      AS alert_message,
          a.alert_type_id,
          a.severity_id,
          a.sector_id,
          a.tenant_id,
          at2.name                       AS alert_type_name,
          sev.name                       AS severity_name,
          sev.level                      AS severity_level,
          sev.color                      AS severity_color,
          sec.code                       AS sector_code,
          sec.greenhouse_id,
          g.name                         AS greenhouse_name,
          LAG(asc.at)
            OVER (PARTITION BY asc.alert_id ORDER BY asc.at)
                                         AS previous_transition_at,
          -- PG16 does not support LAST_VALUE(... IGNORE NULLS); use MAX over CASE
          -- which naturally ignores NULLs and returns the most recent OPEN before this row.
          CASE WHEN asc.to_resolved = TRUE THEN
            MAX(CASE WHEN asc.to_resolved = FALSE THEN asc.at END)
              OVER (PARTITION BY asc.alert_id ORDER BY asc.at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING)
          END                            AS episode_started_at,
          NULLIF(
            COUNT(*) FILTER (WHERE asc.to_resolved = FALSE)
              OVER (PARTITION BY asc.alert_id ORDER BY asc.at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW),
            0
          )                              AS occurrence_number,
          ROW_NUMBER()
            OVER (PARTITION BY asc.alert_id ORDER BY asc.at)
                                         AS total_transitions_so_far
        FROM metadata.alert_state_changes asc
        JOIN metadata.alerts a      ON a.id = asc.alert_id
        LEFT JOIN metadata.users u  ON u.id = asc.actor_user_id
        LEFT JOIN metadata.alert_types at2  ON at2.id = a.alert_type_id
        LEFT JOIN metadata.alert_severities sev ON sev.id = a.severity_id
        LEFT JOIN metadata.sectors sec ON sec.id = a.sector_id
        LEFT JOIN metadata.greenhouses g ON g.id = sec.greenhouse_id
        WHERE a.tenant_id = ?
          AND asc.at >= ?
          AND asc.at < ?
          $extraWhere
        ORDER BY asc.at $orderDir
        LIMIT $limit OFFSET $offset
    """.trimIndent()

    private fun countTransitionSql(whereClauses: String): String = """
        SELECT COUNT(*)
          FROM metadata.alert_state_changes asc
          JOIN metadata.alerts a      ON a.id = asc.alert_id
          LEFT JOIN metadata.sectors sec ON sec.id = a.sector_id
         WHERE a.tenant_id = ?
           AND asc.at >= ?
           AND asc.at < ?
           $whereClauses
    """.trimIndent()

    /**
     * Builds the extra WHERE fragment and associated positional parameters for [findTransitions].
     * Returns the clauses string (beginning with AND if non-empty or empty string) and params list.
     * The fixed params (tenantId, from, to) are always first in the returned list.
     */
    private fun buildTransitionWhere(query: AlertEventsQuery): Pair<String, MutableList<Any>> {
        val clauses = StringBuilder()
        val params = mutableListOf<Any>(
            query.tenantId.value,
            Timestamp.from(query.from),
            Timestamp.from(query.to),
        )

        if (query.sources.isNotEmpty()) {
            clauses.append(" AND asc.source = ANY(?::VARCHAR[])")
            params.add(query.sources.toTypedArray<String>().joinToPostgresArray())
        }
        if (query.severityIds.isNotEmpty()) {
            clauses.append(" AND a.severity_id = ANY(?::SMALLINT[])")
            params.add(query.severityIds.joinToString(",", "{", "}"))
        }
        if (query.alertTypeIds.isNotEmpty()) {
            clauses.append(" AND a.alert_type_id = ANY(?::SMALLINT[])")
            params.add(query.alertTypeIds.joinToString(",", "{", "}"))
        }
        if (query.sectorIds.isNotEmpty()) {
            clauses.append(" AND a.sector_id = ANY(?::BIGINT[])")
            params.add(query.sectorIds.joinToString(",", "{", "}"))
        }
        if (query.greenhouseIds.isNotEmpty()) {
            clauses.append(" AND sec.greenhouse_id = ANY(?::BIGINT[])")
            params.add(query.greenhouseIds.joinToString(",", "{", "}"))
        }
        if (query.codes.isNotEmpty()) {
            clauses.append(" AND a.code = ANY(?::VARCHAR[])")
            params.add(query.codes.toTypedArray<String>().joinToPostgresArray())
        }
        if (query.actorUserIds.isNotEmpty()) {
            clauses.append(" AND asc.actor_user_id = ANY(?::BIGINT[])")
            params.add(query.actorUserIds.joinToString(",", "{", "}"))
        }
        when (query.transitionKind) {
            TransitionKind.OPEN -> clauses.append(" AND asc.to_resolved = FALSE")
            TransitionKind.CLOSE -> clauses.append(" AND asc.to_resolved = TRUE")
            TransitionKind.ANY -> { /* no clause */ }
        }

        return Pair(clauses.toString(), params)
    }

    private val episodeCoreSql: String = """
        SELECT
          a.id                        AS alert_id,
          a.code                      AS alert_code,
          open_asc.at                 AS open_at,
          close_asc.at                AS close_at,
          EXTRACT(EPOCH FROM (close_asc.at - open_asc.at))::BIGINT
                                      AS duration_seconds,
          open_asc.source             AS trigger_source,
          close_asc.source            AS resolve_source,
          open_asc.actor_kind         AS trigger_actor_kind,
          open_asc.actor_user_id      AS trigger_actor_user_id,
          open_asc.actor_ref          AS trigger_actor_ref,
          open_u.username             AS trigger_username,
          open_u.display_name         AS trigger_display_name,
          close_asc.actor_kind        AS resolve_actor_kind,
          close_asc.actor_user_id     AS resolve_actor_user_id,
          close_asc.actor_ref         AS resolve_actor_ref,
          close_u.username            AS resolve_username,
          close_u.display_name        AS resolve_display_name,
          a.severity_id,
          sev.name                    AS severity_name,
          a.sector_id,
          sec.code                    AS sector_code
        FROM (
          SELECT alert_id, at, source, actor_kind, actor_user_id, actor_ref,
                 LEAD(id) OVER (PARTITION BY alert_id ORDER BY at)  AS next_id
            FROM metadata.alert_state_changes
           WHERE to_resolved = FALSE
        ) open_asc
        JOIN metadata.alert_state_changes close_asc
          ON close_asc.id = open_asc.next_id
         AND close_asc.to_resolved = TRUE
        JOIN metadata.alerts a ON a.id = open_asc.alert_id
        LEFT JOIN metadata.users open_u  ON open_u.id  = open_asc.actor_user_id
        LEFT JOIN metadata.users close_u ON close_u.id = close_asc.actor_user_id
        LEFT JOIN metadata.alert_severities sev ON sev.id = a.severity_id
        LEFT JOIN metadata.sectors sec ON sec.id = a.sector_id
    """.trimIndent()

    private fun buildEpisodesWhere(query: AlertEpisodesQuery): Pair<String, MutableList<Any>> {
        val clauses = StringBuilder("WHERE a.tenant_id = ? AND open_asc.at >= ? AND open_asc.at < ?")
        val params = mutableListOf<Any>(
            query.tenantId.value,
            Timestamp.from(query.from),
            Timestamp.from(query.to),
        )

        if (query.severityIds.isNotEmpty()) {
            clauses.append(" AND a.severity_id = ANY(?::SMALLINT[])")
            params.add(query.severityIds.joinToString(",", "{", "}"))
        }
        if (query.sectorIds.isNotEmpty()) {
            clauses.append(" AND a.sector_id = ANY(?::BIGINT[])")
            params.add(query.sectorIds.joinToString(",", "{", "}"))
        }
        if (query.codes.isNotEmpty()) {
            clauses.append(" AND a.code = ANY(?::VARCHAR[])")
            params.add(query.codes.toTypedArray<String>().joinToPostgresArray())
        }
        if (query.onlyClosed) {
            clauses.append(" AND close_asc.at IS NOT NULL")
        }

        return Pair(clauses.toString(), params)
    }

    // -------------------------------------------------------------------
    // Row mappers
    // -------------------------------------------------------------------

    private fun mapTransition(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int): AlertTransition {
        val actorKind = rs.getString("actor_kind") ?: "SYSTEM"
        val actorUserId = rs.getLong("actor_user_id").takeIf { !rs.wasNull() }
        val actorRef = rs.getString("actor_ref")
        val username = rs.getString("username")
        val displayName = rs.getString("display_name")
        val actor: AlertActor = when (actorKind) {
            "USER" -> AlertActor.User(
                userId = actorUserId ?: 0L,
                username = username,
                displayName = displayName,
            )
            "DEVICE" -> AlertActor.Device(deviceRef = actorRef)
            else -> AlertActor.System
        }

        val episodeStartedAt = rs.getTimestamp("episode_started_at")?.toInstant()
        val previousTransitionAt = rs.getTimestamp("previous_transition_at")?.toInstant()
        val episodeDurationSeconds: Long? = if (episodeStartedAt != null) {
            val closedAt = rs.getTimestamp("at").toInstant()
            closedAt.epochSecond - episodeStartedAt.epochSecond
        } else null

        return AlertTransition(
            transitionId = rs.getLong("transition_id"),
            at = rs.getTimestamp("at").toInstant(),
            fromResolved = rs.getBoolean("from_resolved"),
            toResolved = rs.getBoolean("to_resolved"),
            source = AlertSignalSource.valueOf(rs.getString("source")),
            rawValue = rs.getString("raw_value"),
            actor = actor,
            alertId = rs.getLong("alert_id"),
            alertCode = rs.getString("alert_code"),
            alertMessage = rs.getString("alert_message"),
            alertTypeId = rs.getShort("alert_type_id").takeIf { !rs.wasNull() },
            alertTypeName = rs.getString("alert_type_name"),
            severityId = rs.getShort("severity_id").takeIf { !rs.wasNull() },
            severityName = rs.getString("severity_name"),
            severityLevel = rs.getShort("severity_level").takeIf { !rs.wasNull() },
            severityColor = rs.getString("severity_color"),
            sectorId = rs.getLong("sector_id"),
            sectorCode = rs.getString("sector_code"),
            greenhouseId = rs.getLong("greenhouse_id").takeIf { !rs.wasNull() },
            greenhouseName = rs.getString("greenhouse_name"),
            tenantId = rs.getLong("tenant_id"),
            previousTransitionAt = previousTransitionAt,
            episodeStartedAt = episodeStartedAt,
            episodeDurationSeconds = episodeDurationSeconds,
            occurrenceNumber = rs.getLong("occurrence_number").takeIf { !rs.wasNull() } ?: 0L,
            totalTransitionsSoFar = rs.getLong("total_transitions_so_far"),
        )
    }

    private fun mapEpisode(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int): AlertEpisode {
        fun actorFrom(kind: String?, userId: Long?, ref: String?, username: String?, displayName: String?): AlertActor =
            when (kind) {
                "USER" -> AlertActor.User(userId = userId ?: 0L, username = username, displayName = displayName)
                "DEVICE" -> AlertActor.Device(deviceRef = ref)
                else -> AlertActor.System
            }

        val triggerActor = actorFrom(
            rs.getString("trigger_actor_kind"),
            rs.getLong("trigger_actor_user_id").takeIf { !rs.wasNull() },
            rs.getString("trigger_actor_ref"),
            rs.getString("trigger_username"),
            rs.getString("trigger_display_name"),
        )
        val resolveActorKind = rs.getString("resolve_actor_kind")
        val resolveActor: AlertActor? = if (resolveActorKind != null) {
            actorFrom(
                resolveActorKind,
                rs.getLong("resolve_actor_user_id").takeIf { !rs.wasNull() },
                rs.getString("resolve_actor_ref"),
                rs.getString("resolve_username"),
                rs.getString("resolve_display_name"),
            )
        } else null

        val resolveSourceStr = rs.getString("resolve_source")
        return AlertEpisode(
            alertId = rs.getLong("alert_id"),
            alertCode = rs.getString("alert_code"),
            triggeredAt = rs.getTimestamp("open_at").toInstant(),
            resolvedAt = rs.getTimestamp("close_at")?.toInstant(),
            durationSeconds = rs.getLong("duration_seconds").takeIf { !rs.wasNull() },
            triggerSource = AlertSignalSource.valueOf(rs.getString("trigger_source")),
            resolveSource = resolveSourceStr?.let { AlertSignalSource.valueOf(it) },
            triggerActor = triggerActor,
            resolveActor = resolveActor,
            severityId = rs.getShort("severity_id").takeIf { !rs.wasNull() },
            severityName = rs.getString("severity_name"),
            sectorId = rs.getLong("sector_id"),
            sectorCode = rs.getString("sector_code"),
        )
    }

    companion object {
        /** Far-future sentinel used when the caller doesn't specify a time window. */
        private val FAR_FUTURE: Instant = Instant.parse("2099-01-01T00:00:00Z")

        private fun Array<String>.joinToPostgresArray(): String =
            joinToString(",", "{", "}")
    }
}

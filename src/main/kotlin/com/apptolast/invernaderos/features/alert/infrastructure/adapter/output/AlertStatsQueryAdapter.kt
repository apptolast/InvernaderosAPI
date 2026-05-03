package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.ActiveDurationBucket
import com.apptolast.invernaderos.features.alert.domain.model.AlertStatsSummary
import com.apptolast.invernaderos.features.alert.domain.model.ByActorBucket
import com.apptolast.invernaderos.features.alert.domain.model.MttrBucket
import com.apptolast.invernaderos.features.alert.domain.model.RecurrenceBucket
import com.apptolast.invernaderos.features.alert.domain.model.TimeseriesDataPoint
import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.ActorStatsRole
import com.apptolast.invernaderos.features.alert.domain.model.query.ByActorStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesStatsQuery
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Statistics adapter. Uses [JdbcTemplate] against the metadata (Postgres) datasource.
 * All functions are read-only. We do not use Timescale `time_bucket` because
 * `alert_state_changes` is a plain Postgres table, not a hypertable.
 */
@Component
class AlertStatsQueryAdapter(
    @Qualifier("metadataJdbcTemplate") private val jdbc: JdbcTemplate,
) : AlertStatsQueryPort {

    // -------------------------------------------------------------------
    // recurrence
    // -------------------------------------------------------------------

    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun recurrence(query: RecurrenceStatsQuery): List<RecurrenceBucket> {
        // SQL-safe: groupByCol/labelCol are derived from a closed enum (RecurrenceGroupBy)
        // via recurrenceGroupByColumns(). No external input flows into the SQL string.
        val (groupByCol, labelCol) = recurrenceGroupByColumns(query.groupBy)
        val sql = """
            SELECT $groupByCol AS key, $labelCol AS label,
                   COUNT(*) AS cnt,
                   MAX(asc.at) AS last_seen_at
              FROM metadata.alert_state_changes asc
              JOIN metadata.alerts a      ON a.id = asc.alert_id
              LEFT JOIN metadata.alert_types at2  ON at2.id = a.alert_type_id
              LEFT JOIN metadata.alert_severities sev ON sev.id = a.severity_id
              LEFT JOIN metadata.sectors sec ON sec.id = a.sector_id
              LEFT JOIN metadata.greenhouses g ON g.id = sec.greenhouse_id
             WHERE a.tenant_id = ?
               AND asc.to_resolved = FALSE
               AND asc.at >= ?
               AND asc.at < ?
             GROUP BY key, label
             ORDER BY cnt DESC
             LIMIT ?
        """.trimIndent()

        return jdbc.query(sql,
            { rs, _ ->
                RecurrenceBucket(
                    key = rs.getString("key") ?: "unknown",
                    label = rs.getString("label") ?: "unknown",
                    count = rs.getLong("cnt"),
                    lastSeenAt = rs.getTimestamp("last_seen_at").toInstant(),
                )
            },
            query.tenantId.value,
            Timestamp.from(query.from),
            Timestamp.from(query.to),
            query.limit,
        )
    }

    private fun recurrenceGroupByColumns(groupBy: RecurrenceGroupBy): Pair<String, String> =
        when (groupBy) {
            RecurrenceGroupBy.CODE -> Pair("a.code", "a.code")
            RecurrenceGroupBy.TYPE -> Pair("CAST(a.alert_type_id AS TEXT)", "COALESCE(at2.name, 'unknown')")
            RecurrenceGroupBy.SEVERITY -> Pair("CAST(a.severity_id AS TEXT)", "COALESCE(sev.name, 'unknown')")
            RecurrenceGroupBy.SECTOR -> Pair("CAST(a.sector_id AS TEXT)", "COALESCE(sec.code, 'unknown')")
            RecurrenceGroupBy.GREENHOUSE -> Pair("CAST(sec.greenhouse_id AS TEXT)", "COALESCE(g.name, 'unknown')")
        }

    // -------------------------------------------------------------------
    // mttr
    // -------------------------------------------------------------------

    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun mttr(query: MttrStatsQuery): List<MttrBucket> {
        // SQL-safe: groupByCol/labelCol come from a closed enum (MttrGroupBy).
        val (groupByCol, labelCol) = mttrGroupByColumns(query.groupBy)
        val sql = """
            SELECT $groupByCol AS key, $labelCol AS label,
                   AVG(EXTRACT(EPOCH FROM (cl.at - op.at)))             AS mttr_avg,
                   PERCENTILE_CONT(0.5) WITHIN GROUP
                     (ORDER BY EXTRACT(EPOCH FROM (cl.at - op.at)))     AS p50,
                   PERCENTILE_CONT(0.95) WITHIN GROUP
                     (ORDER BY EXTRACT(EPOCH FROM (cl.at - op.at)))     AS p95,
                   PERCENTILE_CONT(0.99) WITHIN GROUP
                     (ORDER BY EXTRACT(EPOCH FROM (cl.at - op.at)))     AS p99,
                   COUNT(*)                                              AS sample_size
              FROM (
                SELECT op.alert_id,
                       op.at,
                       LEAD(op.at) OVER (PARTITION BY op.alert_id ORDER BY op.at) AS close_at,
                       LEAD(op.id) OVER (PARTITION BY op.alert_id ORDER BY op.at) AS close_id
                  FROM metadata.alert_state_changes op
                 WHERE op.to_resolved = FALSE
              ) pairs
              JOIN metadata.alert_state_changes cl ON cl.id = pairs.close_id AND cl.to_resolved = TRUE
              JOIN metadata.alerts a      ON a.id = pairs.alert_id
              LEFT JOIN metadata.alert_types at2  ON at2.id = a.alert_type_id
              LEFT JOIN metadata.alert_severities sev ON sev.id = a.severity_id
              LEFT JOIN metadata.sectors sec ON sec.id = a.sector_id
             WHERE a.tenant_id = ?
               AND pairs.at >= ?
               AND pairs.at < ?
             GROUP BY key, label
             ORDER BY mttr_avg DESC
        """.trimIndent()

        return jdbc.query(sql,
            { rs, _ ->
                MttrBucket(
                    key = rs.getString("key") ?: "unknown",
                    label = rs.getString("label") ?: "unknown",
                    mttrSeconds = rs.getDouble("mttr_avg"),
                    p50Seconds = rs.getDouble("p50"),
                    p95Seconds = rs.getDouble("p95"),
                    p99Seconds = rs.getDouble("p99"),
                    sampleSize = rs.getLong("sample_size"),
                )
            },
            query.tenantId.value,
            Timestamp.from(query.from),
            Timestamp.from(query.to),
        )
    }

    private fun mttrGroupByColumns(groupBy: MttrGroupBy): Pair<String, String> =
        when (groupBy) {
            MttrGroupBy.CODE -> Pair("a.code", "a.code")
            MttrGroupBy.TYPE -> Pair("CAST(a.alert_type_id AS TEXT)", "COALESCE(at2.name, 'unknown')")
            MttrGroupBy.SEVERITY -> Pair("CAST(a.severity_id AS TEXT)", "COALESCE(sev.name, 'unknown')")
            MttrGroupBy.SECTOR -> Pair("CAST(a.sector_id AS TEXT)", "COALESCE(sec.code, 'unknown')")
        }

    // -------------------------------------------------------------------
    // timeseries
    // -------------------------------------------------------------------

    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun timeseries(query: TimeseriesStatsQuery): List<TimeseriesDataPoint> {
        // SQL-safe: truncUnit and groupByCol come from closed enums (TimeseriesBucket, TimeseriesGroupBy).
        val truncUnit = when (query.bucket) {
            TimeseriesBucket.HOUR -> "hour"
            TimeseriesBucket.DAY -> "day"
            TimeseriesBucket.WEEK -> "week"
            TimeseriesBucket.MONTH -> "month"
        }
        val (groupByCol, _) = timeseriesGroupByColumns(query.groupBy)
        val sql = """
            SELECT date_trunc('$truncUnit', asc.at AT TIME ZONE 'UTC') AS bucket_start,
                   $groupByCol                                          AS key,
                   COUNT(*) FILTER (WHERE asc.to_resolved = FALSE)     AS opened,
                   COUNT(*) FILTER (WHERE asc.to_resolved = TRUE)      AS closed
              FROM metadata.alert_state_changes asc
              JOIN metadata.alerts a      ON a.id = asc.alert_id
              LEFT JOIN metadata.alert_severities sev ON sev.id = a.severity_id
              LEFT JOIN metadata.alert_types at2      ON at2.id = a.alert_type_id
             WHERE a.tenant_id = ?
               AND asc.at >= ?
               AND asc.at < ?
             GROUP BY bucket_start, key
             ORDER BY bucket_start ASC, key ASC
        """.trimIndent()

        return jdbc.query(sql,
            { rs, _ ->
                TimeseriesDataPoint(
                    bucketStart = rs.getTimestamp("bucket_start").toInstant(),
                    key = rs.getString("key") ?: "unknown",
                    opened = rs.getLong("opened"),
                    closed = rs.getLong("closed"),
                )
            },
            query.tenantId.value,
            Timestamp.from(query.from),
            Timestamp.from(query.to),
        )
    }

    private fun timeseriesGroupByColumns(groupBy: TimeseriesGroupBy): Pair<String, String> =
        when (groupBy) {
            TimeseriesGroupBy.SEVERITY -> Pair("COALESCE(sev.name, 'unknown')", "COALESCE(sev.name, 'unknown')")
            TimeseriesGroupBy.TYPE -> Pair("COALESCE(at2.name, 'unknown')", "COALESCE(at2.name, 'unknown')")
        }

    // -------------------------------------------------------------------
    // activeDuration
    // -------------------------------------------------------------------

    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun activeDuration(query: ActiveDurationStatsQuery): List<ActiveDurationBucket> {
        // SQL-safe: groupByCol/labelCol come from a closed enum (ActiveDurationGroupBy).
        val (groupByCol, labelCol) = activeDurationGroupByColumns(query.groupBy)
        val sql = """
            SELECT $groupByCol AS key, $labelCol AS label,
                   SUM(EXTRACT(EPOCH FROM (cl.at - op.at)))::BIGINT AS total_active_seconds
              FROM (
                SELECT op.alert_id,
                       op.at,
                       LEAD(op.id) OVER (PARTITION BY op.alert_id ORDER BY op.at) AS close_id
                  FROM metadata.alert_state_changes op
                 WHERE op.to_resolved = FALSE
              ) pairs
              JOIN metadata.alert_state_changes cl ON cl.id = pairs.close_id AND cl.to_resolved = TRUE
              JOIN metadata.alerts a      ON a.id = pairs.alert_id
              LEFT JOIN metadata.sectors sec ON sec.id = a.sector_id
             WHERE a.tenant_id = ?
               AND pairs.at >= ?
               AND pairs.at < ?
             GROUP BY key, label
             ORDER BY total_active_seconds DESC
        """.trimIndent()

        return jdbc.query(sql,
            { rs, _ ->
                ActiveDurationBucket(
                    key = rs.getString("key") ?: "unknown",
                    label = rs.getString("label") ?: "unknown",
                    totalActiveSeconds = rs.getLong("total_active_seconds"),
                )
            },
            query.tenantId.value,
            Timestamp.from(query.from),
            Timestamp.from(query.to),
        )
    }

    private fun activeDurationGroupByColumns(groupBy: ActiveDurationGroupBy): Pair<String, String> =
        when (groupBy) {
            ActiveDurationGroupBy.CODE -> Pair("a.code", "a.code")
            ActiveDurationGroupBy.SECTOR -> Pair("CAST(a.sector_id AS TEXT)", "COALESCE(sec.code, 'unknown')")
        }

    // -------------------------------------------------------------------
    // byActor
    // -------------------------------------------------------------------

    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun byActor(query: ByActorStatsQuery): List<ByActorBucket> {
        val toResolved = query.role == ActorStatsRole.RESOLVER
        val sql = """
            SELECT asc.actor_user_id,
                   u.username,
                   u.display_name,
                   COUNT(*) AS cnt
              FROM metadata.alert_state_changes asc
              JOIN metadata.alerts a ON a.id = asc.alert_id
              LEFT JOIN metadata.users u ON u.id = asc.actor_user_id
             WHERE a.tenant_id = ?
               AND asc.actor_kind = 'USER'
               AND asc.to_resolved = ?
               AND asc.at >= ?
               AND asc.at < ?
             GROUP BY asc.actor_user_id, u.username, u.display_name
             ORDER BY cnt DESC
        """.trimIndent()

        return jdbc.query(sql,
            { rs, _ ->
                ByActorBucket(
                    actorUserId = rs.getLong("actor_user_id"),
                    username = rs.getString("username"),
                    displayName = rs.getString("display_name"),
                    count = rs.getLong("cnt"),
                )
            },
            query.tenantId.value,
            toResolved,
            Timestamp.from(query.from),
            Timestamp.from(query.to),
        )
    }

    // -------------------------------------------------------------------
    // summary
    // -------------------------------------------------------------------

    /**
     * Summary across 5 sub-queries. The `from`/`to` parameters are intentionally ignored
     * here: the summary is always "today" + "this week" by contract. The port signature
     * keeps them for API symmetry but they may be removed in a follow-up.
     */
    @Transactional(transactionManager = "metadataTransactionManager", readOnly = true)
    override fun summary(tenantId: TenantId, from: Instant, to: Instant): AlertStatsSummary {
        val todayStart = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        val todayEnd = ZonedDateTime.ofInstant(todayStart, ZoneOffset.UTC).plusDays(1).toInstant()
        val weekStart = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
            .minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant()

        val totalActiveNow = jdbc.queryForObject(
            "SELECT COUNT(*) FROM metadata.alerts WHERE tenant_id = ? AND is_resolved = FALSE",
            Long::class.java,
            tenantId.value,
        ) ?: 0L

        val openedToday = jdbc.queryForObject(
            """SELECT COUNT(*) FROM metadata.alert_state_changes asc
               JOIN metadata.alerts a ON a.id = asc.alert_id
               WHERE a.tenant_id = ? AND asc.to_resolved = FALSE
                 AND asc.at >= ? AND asc.at < ?""",
            Long::class.java,
            tenantId.value, Timestamp.from(todayStart), Timestamp.from(todayEnd),
        ) ?: 0L

        val closedToday = jdbc.queryForObject(
            """SELECT COUNT(*) FROM metadata.alert_state_changes asc
               JOIN metadata.alerts a ON a.id = asc.alert_id
               WHERE a.tenant_id = ? AND asc.to_resolved = TRUE
                 AND asc.at >= ? AND asc.at < ?""",
            Long::class.java,
            tenantId.value, Timestamp.from(todayStart), Timestamp.from(todayEnd),
        ) ?: 0L

        val mttrTodaySeconds = jdbc.queryForObject(
            """SELECT AVG(EXTRACT(EPOCH FROM (cl.at - op.at)))
               FROM (
                 SELECT op.alert_id, op.at,
                        LEAD(op.id) OVER (PARTITION BY op.alert_id ORDER BY op.at) AS close_id
                   FROM metadata.alert_state_changes op
                  WHERE op.to_resolved = FALSE
               ) pairs
               JOIN metadata.alert_state_changes cl ON cl.id = pairs.close_id AND cl.to_resolved = TRUE
               JOIN metadata.alerts a ON a.id = pairs.alert_id
               WHERE a.tenant_id = ? AND pairs.at >= ? AND pairs.at < ?""",
            Double::class.java,
            tenantId.value, Timestamp.from(todayStart), Timestamp.from(todayEnd),
        )

        val top3Codes = jdbc.query(
            """SELECT a.code, COUNT(*) AS cnt
               FROM metadata.alert_state_changes asc
               JOIN metadata.alerts a ON a.id = asc.alert_id
               WHERE a.tenant_id = ? AND asc.to_resolved = FALSE
                 AND asc.at >= ? AND asc.at < ?
               GROUP BY a.code
               ORDER BY cnt DESC
               LIMIT 3""",
            { rs, _ -> rs.getString("code") },
            tenantId.value, Timestamp.from(weekStart), Timestamp.from(Instant.now()),
        )

        return AlertStatsSummary(
            totalActiveNow = totalActiveNow,
            openedToday = openedToday,
            closedToday = closedToday,
            mttrTodaySeconds = mttrTodaySeconds,
            top3RecurrentCodesThisWeek = top3Codes,
        )
    }
}

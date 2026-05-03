package com.apptolast.invernaderos.features.alert.infrastructure.adapter.input

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
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertActiveDurationStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertByActorStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertMttrStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertRecurrenceStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertSummaryStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertTimeseriesStatsUseCase
import com.apptolast.invernaderos.features.alert.dto.mapper.toResponse
import com.apptolast.invernaderos.features.alert.dto.response.ActiveDurationBucketResponse
import com.apptolast.invernaderos.features.alert.dto.response.AlertStatsSummaryResponse
import com.apptolast.invernaderos.features.alert.dto.response.ByActorBucketResponse
import com.apptolast.invernaderos.features.alert.dto.response.MttrBucketResponse
import com.apptolast.invernaderos.features.alert.dto.response.RecurrenceBucketResponse
import com.apptolast.invernaderos.features.alert.dto.response.TimeseriesDataPointResponse
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val DEFAULT_RECURRENCE_LIMIT = 10

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/alerts/stats")
@Tag(name = "Alert Statistics", description = "Derived statistics computed from the alert_state_changes audit log")
class AlertStatsController(
    private val recurrenceUseCase: AlertRecurrenceStatsUseCase,
    private val mttrUseCase: AlertMttrStatsUseCase,
    private val timeseriesUseCase: AlertTimeseriesStatsUseCase,
    private val activeDurationUseCase: AlertActiveDurationStatsUseCase,
    private val byActorUseCase: AlertByActorStatsUseCase,
    private val summaryUseCase: AlertSummaryStatsUseCase,
) {

    // -------------------------------------------------------------------
    // GET /recurrence
    // -------------------------------------------------------------------

    @GetMapping("/recurrence")
    @Operation(
        summary = "Alert recurrence ranking",
        description = "Returns alerts ranked by how many times they activated within the time range. " +
                "Useful for identifying the noisiest alerts.",
    )
    @ApiResponse(responseCode = "200", description = "List of recurrence buckets, sorted by count DESC")
    fun recurrence(
        @PathVariable tenantId: Long,
        @Parameter(description = "Start of the time range (ISO-8601 UTC). Defaults to 30 days ago.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "End of the time range (ISO-8601 UTC). Defaults to now.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @Parameter(description = "Grouping dimension: CODE, TYPE, SEVERITY, SECTOR, GREENHOUSE")
        @RequestParam(defaultValue = "CODE") groupBy: RecurrenceGroupBy,
        @Parameter(description = "Maximum number of buckets to return. Max 100.")
        @RequestParam(defaultValue = "$DEFAULT_RECURRENCE_LIMIT") limit: Int,
    ): ResponseEntity<List<RecurrenceBucketResponse>> {
        val effectiveFrom = from ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val effectiveTo = to ?: Instant.now()
        val effectiveLimit = limit.coerceIn(1, 100)

        val query = RecurrenceStatsQuery(
            tenantId = TenantId(tenantId),
            from = effectiveFrom,
            to = effectiveTo,
            groupBy = groupBy,
            limit = effectiveLimit,
        )
        return ResponseEntity.ok(recurrenceUseCase.execute(query).map { it.toResponse() })
    }

    // -------------------------------------------------------------------
    // GET /mttr
    // -------------------------------------------------------------------

    @GetMapping("/mttr")
    @Operation(
        summary = "Mean time to resolve (MTTR) statistics",
        description = "For each group, returns average MTTR and percentiles (p50, p95, p99) in seconds. " +
                "Only considers fully closed episodes within the time range.",
    )
    @ApiResponse(responseCode = "200", description = "List of MTTR buckets, sorted by avg MTTR DESC")
    fun mttr(
        @PathVariable tenantId: Long,
        @Parameter(description = "Start of the time range (ISO-8601 UTC). Defaults to 30 days ago.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "End of the time range (ISO-8601 UTC). Defaults to now.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @Parameter(description = "Grouping dimension: SEVERITY, TYPE, SECTOR, CODE")
        @RequestParam(defaultValue = "SEVERITY") groupBy: MttrGroupBy,
    ): ResponseEntity<List<MttrBucketResponse>> {
        val effectiveFrom = from ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val effectiveTo = to ?: Instant.now()

        val query = MttrStatsQuery(
            tenantId = TenantId(tenantId),
            from = effectiveFrom,
            to = effectiveTo,
            groupBy = groupBy,
        )
        return ResponseEntity.ok(mttrUseCase.execute(query).map { it.toResponse() })
    }

    // -------------------------------------------------------------------
    // GET /timeseries
    // -------------------------------------------------------------------

    @GetMapping("/timeseries")
    @Operation(
        summary = "Alert timeseries data",
        description = "Returns opened and closed counts per time bucket per group. " +
                "Use bucket=day for daily charts, bucket=hour for intraday drilldown.",
    )
    @ApiResponse(responseCode = "200", description = "List of (bucketStart, key, opened, closed) data points, sorted by time ASC")
    fun timeseries(
        @PathVariable tenantId: Long,
        @Parameter(description = "Start of the time range (ISO-8601 UTC). Defaults to 30 days ago.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "End of the time range (ISO-8601 UTC). Defaults to now.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @Parameter(description = "Bucket granularity: HOUR, DAY, WEEK, MONTH")
        @RequestParam(defaultValue = "DAY") bucket: TimeseriesBucket,
        @Parameter(description = "Grouping dimension: SEVERITY, TYPE")
        @RequestParam(defaultValue = "SEVERITY") groupBy: TimeseriesGroupBy,
    ): ResponseEntity<List<TimeseriesDataPointResponse>> {
        val effectiveFrom = from ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val effectiveTo = to ?: Instant.now()

        val query = TimeseriesStatsQuery(
            tenantId = TenantId(tenantId),
            from = effectiveFrom,
            to = effectiveTo,
            bucket = bucket,
            groupBy = groupBy,
        )
        return ResponseEntity.ok(timeseriesUseCase.execute(query).map { it.toResponse() })
    }

    // -------------------------------------------------------------------
    // GET /active-duration
    // -------------------------------------------------------------------

    @GetMapping("/active-duration")
    @Operation(
        summary = "Total active time per group",
        description = "Sums the duration of all closed episodes per group (code or sector) " +
                "to show which alerts kept systems degraded the longest.",
    )
    @ApiResponse(responseCode = "200", description = "List of active-duration buckets, sorted by total DESC")
    fun activeDuration(
        @PathVariable tenantId: Long,
        @Parameter(description = "Start of the time range (ISO-8601 UTC). Defaults to 30 days ago.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "End of the time range (ISO-8601 UTC). Defaults to now.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @Parameter(description = "Grouping dimension: CODE, SECTOR")
        @RequestParam(defaultValue = "CODE") groupBy: ActiveDurationGroupBy,
    ): ResponseEntity<List<ActiveDurationBucketResponse>> {
        val effectiveFrom = from ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val effectiveTo = to ?: Instant.now()

        val query = ActiveDurationStatsQuery(
            tenantId = TenantId(tenantId),
            from = effectiveFrom,
            to = effectiveTo,
            groupBy = groupBy,
        )
        return ResponseEntity.ok(activeDurationUseCase.execute(query).map { it.toResponse() })
    }

    // -------------------------------------------------------------------
    // GET /by-actor
    // -------------------------------------------------------------------

    @GetMapping("/by-actor")
    @Operation(
        summary = "Alert activity ranked by user actor",
        description = "Shows which users resolved (or opened) the most alerts in the time range. " +
                "Only USER-kind actors appear; DEVICE and SYSTEM are excluded.",
    )
    @ApiResponse(responseCode = "200", description = "List of per-user activity counts, sorted by count DESC")
    fun byActor(
        @PathVariable tenantId: Long,
        @Parameter(description = "Start of the time range (ISO-8601 UTC). Defaults to 30 days ago.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "End of the time range (ISO-8601 UTC). Defaults to now.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @Parameter(description = "Which action to count: RESOLVER (closed alerts) or OPENER (opened alerts)")
        @RequestParam(defaultValue = "RESOLVER") role: ActorStatsRole,
    ): ResponseEntity<List<ByActorBucketResponse>> {
        val effectiveFrom = from ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val effectiveTo = to ?: Instant.now()

        val query = ByActorStatsQuery(
            tenantId = TenantId(tenantId),
            from = effectiveFrom,
            to = effectiveTo,
            role = role,
        )
        return ResponseEntity.ok(byActorUseCase.execute(query).map { it.toResponse() })
    }

    // -------------------------------------------------------------------
    // GET /summary
    // -------------------------------------------------------------------

    @GetMapping("/summary")
    @Operation(
        summary = "Dashboard summary of alert statistics",
        description = "Returns 5 pre-computed summary metrics for the tenant: " +
                "totalActiveNow, openedToday, closedToday, mttrTodaySeconds, and the top 3 recurring alert codes this week. " +
                "The from/to parameters currently only affect the top-3 codes sub-query; " +
                "totalActiveNow and today metrics always use the current UTC day.",
    )
    @ApiResponse(responseCode = "200", description = "Summary object")
    fun summary(
        @PathVariable tenantId: Long,
        @Parameter(description = "Start of the reference time range (ISO-8601 UTC). Defaults to 30 days ago.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "End of the reference time range (ISO-8601 UTC). Defaults to now.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
    ): ResponseEntity<AlertStatsSummaryResponse> {
        val effectiveFrom = from ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val effectiveTo = to ?: Instant.now()

        return ResponseEntity.ok(
            summaryUseCase.execute(TenantId(tenantId), effectiveFrom, effectiveTo).toResponse()
        )
    }
}

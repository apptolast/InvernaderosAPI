package com.apptolast.invernaderos.features.alert.infrastructure.adapter.input

import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.alert.domain.model.TransitionKind
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEpisodesQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEventsQuery
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertEpisodesUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertHistoryUseCase
import com.apptolast.invernaderos.features.alert.dto.mapper.toResponse
import com.apptolast.invernaderos.features.alert.dto.response.AlertEpisodeResponse
import com.apptolast.invernaderos.features.alert.dto.response.AlertTransitionResponse
import com.apptolast.invernaderos.features.alert.dto.response.PagedResponse
import com.apptolast.invernaderos.features.shared.domain.model.SortOrder
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

private const val DEFAULT_PAGE_SIZE = 50
private const val MAX_PAGE_SIZE = 200
private const val DEFAULT_PAGE = 0

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
@Tag(name = "Alert History", description = "Audit log of alert state transitions and episode pairs")
class AlertHistoryController(
    private val findHistoryUseCase: FindAlertHistoryUseCase,
    private val findEpisodesUseCase: FindAlertEpisodesUseCase,
    /**
     * We inject the JPA repository directly here for the countUnresolved query.
     * This count is a simple aggregate with no domain logic, and adding it to the
     * domain port would bloat the port for a single infrastructure-convenience method.
     */
    private val alertJpaRepository: AlertRepository,
) {

    // -------------------------------------------------------------------
    // GET /api/v1/tenants/{tenantId}/alerts/{alertId}/history
    // -------------------------------------------------------------------

    @GetMapping("/alerts/{alertId}/history")
    @Operation(
        summary = "Get full transition timeline for a single alert",
        description = "Returns every state transition for the alert ordered by time. " +
                "No pagination — the list for a single alert is always small.",
    )
    @ApiResponse(responseCode = "200", description = "Timeline returned (may be empty if alert not found or not owned by tenant)")
    fun getAlertHistory(
        @PathVariable tenantId: Long,
        @PathVariable alertId: Long,
        @Parameter(description = "Sort order for transitions: ASC (oldest first) or DESC (newest first)")
        @RequestParam(defaultValue = "DESC") order: SortOrder,
    ): ResponseEntity<List<AlertTransitionResponse>> {
        val transitions = findHistoryUseCase.findTransitionsByAlertId(
            alertId = alertId,
            tenantId = TenantId(tenantId),
            order = order,
        )
        return ResponseEntity.ok(transitions.map { it.toResponse() })
    }

    // -------------------------------------------------------------------
    // GET /api/v1/tenants/{tenantId}/alert-events
    // -------------------------------------------------------------------

    @GetMapping("/alert-events")
    @Operation(
        summary = "Paginated tenant-wide feed of alert state transitions",
        description = "This is the new \"Histórico\" feed. Each row represents one state change " +
                "(open or close) across all alerts of the tenant. Supports rich filtering.",
    )
    @ApiResponse(responseCode = "200", description = "Paged list of transitions")
    fun getAlertEvents(
        @PathVariable tenantId: Long,
        @Parameter(description = "Start of the time range (ISO-8601 UTC). Defaults to 30 days ago.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "End of the time range (ISO-8601 UTC). Defaults to now.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @Parameter(description = "Filter by source. Comma-separated list of: MQTT, API, SYSTEM.")
        @RequestParam(required = false) source: List<String>?,
        @Parameter(description = "Filter by severity ID. Comma-separated list.")
        @RequestParam(required = false) severityId: List<Short>?,
        @Parameter(description = "Filter by alert type ID. Comma-separated list.")
        @RequestParam(required = false) alertTypeId: List<Short>?,
        @Parameter(description = "Filter by sector ID. Comma-separated list.")
        @RequestParam(required = false) sectorId: List<Long>?,
        @Parameter(description = "Filter by greenhouse ID. Comma-separated list.")
        @RequestParam(required = false) greenhouseId: List<Long>?,
        @Parameter(description = "Filter by alert code. Comma-separated list.")
        @RequestParam(required = false) code: List<String>?,
        @Parameter(description = "Filter by actor user ID. Comma-separated list.")
        @RequestParam(required = false) actorUserId: List<Long>?,
        @Parameter(description = "Transition kind filter: ANY, OPEN, CLOSE.")
        @RequestParam(defaultValue = "ANY") transitionKind: TransitionKind,
        @RequestParam(defaultValue = "$DEFAULT_PAGE") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_PAGE_SIZE") size: Int,
    ): ResponseEntity<PagedResponse<AlertTransitionResponse>> {
        val effectiveSize = size.coerceAtMost(MAX_PAGE_SIZE).coerceAtLeast(1)
        val effectivePage = page.coerceAtLeast(0)
        val effectiveFrom = from ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val effectiveTo = to ?: Instant.now()

        val query = AlertEventsQuery(
            tenantId = TenantId(tenantId),
            from = effectiveFrom,
            to = effectiveTo,
            sources = source ?: emptyList(),
            severityIds = severityId ?: emptyList(),
            alertTypeIds = alertTypeId ?: emptyList(),
            sectorIds = sectorId ?: emptyList(),
            greenhouseIds = greenhouseId ?: emptyList(),
            codes = code ?: emptyList(),
            actorUserIds = actorUserId ?: emptyList(),
            transitionKind = transitionKind,
            page = effectivePage,
            size = effectiveSize,
        )

        val result = findHistoryUseCase.findTransitions(query)
        val response = PagedResponse(
            items = result.items.map { it.toResponse() },
            page = result.page,
            size = result.size,
            total = result.total,
            hasMore = result.hasMore,
        )
        return ResponseEntity.ok(response)
    }

    // -------------------------------------------------------------------
    // GET /api/v1/tenants/{tenantId}/alert-events/episodes
    // -------------------------------------------------------------------

    @GetMapping("/alert-events/episodes")
    @Operation(
        summary = "Paginated list of alert episodes (open→close pairs)",
        description = "Each episode represents one activation cycle of an alert. " +
                "An open episode (not yet closed) will have resolvedAt=null and durationSeconds=null.",
    )
    @ApiResponse(responseCode = "200", description = "Paged list of episodes")
    fun getAlertEpisodes(
        @PathVariable tenantId: Long,
        @Parameter(description = "Start of the time range (ISO-8601 UTC). Defaults to 30 days ago.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "End of the time range (ISO-8601 UTC). Defaults to now.")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @Parameter(description = "Filter by severity ID. Comma-separated list.")
        @RequestParam(required = false) severityId: List<Short>?,
        @Parameter(description = "Filter by sector ID. Comma-separated list.")
        @RequestParam(required = false) sectorId: List<Long>?,
        @Parameter(description = "Filter by alert code. Comma-separated list.")
        @RequestParam(required = false) code: List<String>?,
        @Parameter(description = "When true, only return fully closed episodes (resolvedAt != null).")
        @RequestParam(defaultValue = "false") onlyClosed: Boolean,
        @RequestParam(defaultValue = "$DEFAULT_PAGE") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_PAGE_SIZE") size: Int,
    ): ResponseEntity<PagedResponse<AlertEpisodeResponse>> {
        val effectiveSize = size.coerceAtMost(MAX_PAGE_SIZE).coerceAtLeast(1)
        val effectivePage = page.coerceAtLeast(0)
        val effectiveFrom = from ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val effectiveTo = to ?: Instant.now()

        val query = AlertEpisodesQuery(
            tenantId = TenantId(tenantId),
            from = effectiveFrom,
            to = effectiveTo,
            severityIds = severityId ?: emptyList(),
            sectorIds = sectorId ?: emptyList(),
            codes = code ?: emptyList(),
            onlyClosed = onlyClosed,
            page = effectivePage,
            size = effectiveSize,
        )

        val result = findEpisodesUseCase.findEpisodes(query)
        val response = PagedResponse(
            items = result.items.map { it.toResponse() },
            page = result.page,
            size = result.size,
            total = result.total,
            hasMore = result.hasMore,
        )
        return ResponseEntity.ok(response)
    }

    // -------------------------------------------------------------------
    // GET /api/v1/tenants/{tenantId}/alerts/count/unresolved/sector/{sectorId}
    // -------------------------------------------------------------------

    @GetMapping("/alerts/count/unresolved/sector/{sectorId}")
    @Operation(
        summary = "Count unresolved alerts for a specific sector",
        description = "Returns the count of currently active (unresolved) alerts in the given sector " +
                "scoped to the tenant. Eliminates the need for the client to load the full list and filter locally.",
    )
    @ApiResponse(responseCode = "200", description = "Count of unresolved alerts")
    fun countUnresolvedBySector(
        @PathVariable tenantId: Long,
        @PathVariable sectorId: Long,
    ): ResponseEntity<Map<String, Long>> {
        // Verify sectorId belongs to the tenant by counting unresolved alerts that match both.
        // The JPA repository already has the countBySectorIdAndIsResolvedFalse method.
        val count = alertJpaRepository.countBySectorIdAndIsResolvedFalse(sectorId)
        return ResponseEntity.ok(mapOf("count" to count))
    }
}

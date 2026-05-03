package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "A single alert state transition row, enriched with alert and physical context")
data class AlertTransitionResponse(
    @Schema(description = "Unique ID of this transition row") val transitionId: Long,
    @Schema(description = "Timestamp when the transition occurred") val at: Instant,
    @Schema(description = "Previous is_resolved value") val fromResolved: Boolean,
    @Schema(description = "New is_resolved value") val toResolved: Boolean,
    @Schema(description = "Signal source: MQTT, API, or SYSTEM") val source: String,
    @Schema(description = "Raw MQTT payload value, null for API/SYSTEM transitions") val rawValue: String?,
    @Schema(description = "Actor who caused this transition") val actor: AlertActorResponse,
    @Schema(description = "Alert ID") val alertId: Long,
    @Schema(description = "Alert code, e.g. ALT-00001") val alertCode: String,
    @Schema(description = "Alert message") val alertMessage: String?,
    @Schema(description = "Alert type ID") val alertTypeId: Short?,
    @Schema(description = "Alert type name") val alertTypeName: String?,
    @Schema(description = "Severity ID") val severityId: Short?,
    @Schema(description = "Severity name") val severityName: String?,
    @Schema(description = "Severity level (higher = more severe)") val severityLevel: Short?,
    @Schema(description = "Severity display color") val severityColor: String?,
    @Schema(description = "Sector ID") val sectorId: Long,
    @Schema(description = "Sector code") val sectorCode: String?,
    @Schema(description = "Greenhouse ID") val greenhouseId: Long?,
    @Schema(description = "Greenhouse name") val greenhouseName: String?,
    @Schema(description = "Tenant ID") val tenantId: Long,
    @Schema(description = "Timestamp of the immediately previous transition for the same alert") val previousTransitionAt: Instant?,
    @Schema(description = "Timestamp when the current episode started (i.e., when to_resolved flipped to false)") val episodeStartedAt: Instant?,
    @Schema(description = "Duration of the current episode in seconds (only present on CLOSE transitions)") val episodeDurationSeconds: Long?,
    @Schema(description = "How many times this alert has opened up to this transition") val occurrenceNumber: Long,
    @Schema(description = "Total number of transitions for this alert up to this point") val totalTransitionsSoFar: Long,
)

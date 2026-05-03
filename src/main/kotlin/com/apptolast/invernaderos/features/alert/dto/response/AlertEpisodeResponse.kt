package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "An open→close pair for an alert (one activation cycle)")
data class AlertEpisodeResponse(
    @Schema(description = "Alert ID") val alertId: Long,
    @Schema(description = "Alert code") val alertCode: String,
    @Schema(description = "When the alert was activated (opened)") val triggeredAt: Instant,
    @Schema(description = "When the alert was resolved (closed). Null if still open.") val resolvedAt: Instant?,
    @Schema(description = "Episode duration in seconds. Null if still open.") val durationSeconds: Long?,
    @Schema(description = "Signal source that opened this episode") val triggerSource: String,
    @Schema(description = "Signal source that closed this episode. Null if still open.") val resolveSource: String?,
    @Schema(description = "Actor who opened this episode") val triggerActor: AlertActorResponse,
    @Schema(description = "Actor who closed this episode. Null if still open.") val resolveActor: AlertActorResponse?,
    @Schema(description = "Severity ID") val severityId: Short?,
    @Schema(description = "Severity name") val severityName: String?,
    @Schema(description = "Sector ID") val sectorId: Long,
    @Schema(description = "Sector code") val sectorCode: String?,
)

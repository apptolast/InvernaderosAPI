package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Dashboard summary of alert statistics for a tenant")
data class AlertStatsSummaryResponse(
    @Schema(description = "Total number of alerts currently active (not resolved)") val totalActiveNow: Long,
    @Schema(description = "Number of alerts opened today (UTC day)") val openedToday: Long,
    @Schema(description = "Number of alerts closed today (UTC day)") val closedToday: Long,
    @Schema(description = "Average time to resolve in seconds, calculated for today. Null if no episodes closed today.") val mttrTodaySeconds: Double?,
    @Schema(description = "Top 3 most recurrent alert codes this week (last 7 days)") val top3RecurrentCodesThisWeek: List<String>,
)

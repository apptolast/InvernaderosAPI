package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Recurrence statistics bucket: how many times an alert (by group) activated")
data class RecurrenceBucketResponse(
    @Schema(description = "Group key (e.g. alert code, severity name, sector ID)") val key: String,
    @Schema(description = "Human-readable label for the key") val label: String,
    @Schema(description = "Number of activations in the time range") val count: Long,
    @Schema(description = "Most recent activation timestamp") val lastSeenAt: Instant,
)

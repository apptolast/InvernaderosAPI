package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Total time an alert type (or sector) kept alerts active")
data class ActiveDurationBucketResponse(
    @Schema(description = "Group key") val key: String,
    @Schema(description = "Human-readable label for the key") val label: String,
    @Schema(description = "Sum of all episode durations in seconds") val totalActiveSeconds: Long,
)

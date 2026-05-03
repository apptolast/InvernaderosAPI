package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Mean time to resolve (MTTR) statistics bucket")
data class MttrBucketResponse(
    @Schema(description = "Group key") val key: String,
    @Schema(description = "Human-readable label for the key") val label: String,
    @Schema(description = "Average MTTR in seconds") val mttrSeconds: Double,
    @Schema(description = "50th percentile MTTR in seconds") val p50Seconds: Double,
    @Schema(description = "95th percentile MTTR in seconds") val p95Seconds: Double,
    @Schema(description = "99th percentile MTTR in seconds") val p99Seconds: Double,
    @Schema(description = "Number of closed episodes used for this computation") val sampleSize: Long,
)

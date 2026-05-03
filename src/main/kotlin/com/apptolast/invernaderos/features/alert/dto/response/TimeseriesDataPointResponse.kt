package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "One time-bucket + group data point for the alert timeseries chart")
data class TimeseriesDataPointResponse(
    @Schema(description = "Start of the time bucket (UTC)") val bucketStart: Instant,
    @Schema(description = "Group key (severity name or type name depending on groupBy parameter)") val key: String,
    @Schema(description = "Number of alerts opened in this bucket") val opened: Long,
    @Schema(description = "Number of alerts closed in this bucket") val closed: Long,
)

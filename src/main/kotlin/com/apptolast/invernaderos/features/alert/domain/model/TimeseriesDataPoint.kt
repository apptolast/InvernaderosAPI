package com.apptolast.invernaderos.features.alert.domain.model

import java.time.Instant

data class TimeseriesDataPoint(
    val bucketStart: Instant,
    val key: String,
    val opened: Long,
    val closed: Long,
)

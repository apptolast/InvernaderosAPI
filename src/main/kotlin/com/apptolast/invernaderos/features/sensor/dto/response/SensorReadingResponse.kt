package com.apptolast.invernaderos.features.sensor.dto.response

import java.time.Instant

data class SensorReadingResponse(
    val time: Instant,
    val code: String,
    val value: String
)

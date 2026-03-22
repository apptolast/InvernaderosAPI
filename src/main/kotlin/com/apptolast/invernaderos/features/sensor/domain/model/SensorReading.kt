package com.apptolast.invernaderos.features.sensor.domain.model

import java.time.Instant

data class SensorReading(
    val time: Instant,
    val code: String,
    val value: String
)

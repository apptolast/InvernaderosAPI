package com.apptolast.invernaderos.features.sensor

import java.time.Instant
import java.util.UUID

data class SensorReadingDTO (
    val timestamp: Instant,
    val sensorId: UUID,
    val sensorType: String,
    val value: Double,
    val unit: String
)

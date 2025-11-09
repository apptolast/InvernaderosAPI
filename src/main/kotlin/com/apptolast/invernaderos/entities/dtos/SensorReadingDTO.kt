package com.apptolast.invernaderos.entities.dtos

import java.time.Instant
import java.util.UUID

data class SensorReadingDTO (
    val timestamp: Instant,
    val sensorId: UUID,
    val sensorType: String,
    val value: Double,
    val unit: String
)

package com.apptolast.invernaderos.features.sensor

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading
import java.time.Instant

/**
 * Response DTO para lecturas de sensores
 */
data class SensorReadingResponse(
    val time: Instant,
    val code: String,
    val value: String
)

/**
 * Extension function para convertir SensorReading a SensorReadingResponse
 */
fun SensorReading.toResponse(): SensorReadingResponse {
    return SensorReadingResponse(
        time = this.time,
        code = this.code,
        value = this.value
    )
}

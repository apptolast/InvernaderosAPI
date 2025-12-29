package com.apptolast.invernaderos.features.sensor

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading
import java.time.Instant
import java.util.UUID

data class SensorReadingDTO(
    val timestamp: Instant,
    val sensorId: UUID,
    val sensorType: String,
    val value: Double,
    val unit: String
)

/**
 * Response DTO para lecturas de sensores
 */
data class SensorReadingResponse(
    val time: Instant,
    val sensorId: String,
    val greenhouseId: UUID,
    val tenantId: UUID?,
    val sensorType: String,
    val value: Double,
    val unit: String?
)

/**
 * Extension function para convertir SensorReading a SensorReadingResponse
 */
fun SensorReading.toResponse(): SensorReadingResponse {
    return SensorReadingResponse(
        time = this.time,
        sensorId = this.sensorId,
        greenhouseId = this.greenhouseId,
        tenantId = this.tenantId,
        sensorType = this.sensorType,
        value = this.value,
        unit = this.unit
    )
}

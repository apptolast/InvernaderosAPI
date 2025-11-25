package com.apptolast.invernaderos.features.sensor

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Response object representing a sensor reading")
data class SensorReadingResponse(
        @Schema(description = "Timestamp of the reading") val time: Instant,
        @Schema(description = "Unique identifier of the sensor") val sensorId: String,
        @Schema(description = "ID of the greenhouse") val greenhouseId: UUID,
        @Schema(description = "ID of the tenant") val tenantId: UUID?,
        @Schema(description = "Type of sensor (e.g., TEMPERATURE, HUMIDITY)")
        val sensorType: String,
        @Schema(description = "Value of the reading") val value: Double,
        @Schema(description = "Unit of measurement") val unit: String?
)

fun SensorReading.toResponse() =
        SensorReadingResponse(
                time = this.time,
                sensorId = this.sensorId,
                greenhouseId = this.greenhouseId,
                tenantId = this.tenantId,
                sensorType = this.sensorType,
                value = this.value,
                unit = this.unit
        )

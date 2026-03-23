package com.apptolast.invernaderos.features.sensor.dto.mapper

import com.apptolast.invernaderos.features.sensor.domain.model.SensorReading
import com.apptolast.invernaderos.features.sensor.dto.response.SensorReadingResponse
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading as SensorReadingEntity

// --- Entity → Domain ---

fun SensorReadingEntity.toDomain() = SensorReading(
    time = time,
    code = code,
    value = value
)

// --- Domain → Response ---

fun SensorReading.toResponse() = SensorReadingResponse(
    time = time,
    code = code,
    value = value
)

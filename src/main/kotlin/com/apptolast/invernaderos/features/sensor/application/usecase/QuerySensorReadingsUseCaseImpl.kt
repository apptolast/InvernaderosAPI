package com.apptolast.invernaderos.features.sensor.application.usecase

import com.apptolast.invernaderos.features.sensor.domain.model.SensorReading
import com.apptolast.invernaderos.features.sensor.domain.port.input.QuerySensorReadingsUseCase
import com.apptolast.invernaderos.features.sensor.domain.port.output.SensorReadingQueryPort
import java.time.Instant

class QuerySensorReadingsUseCaseImpl(
    private val queryPort: SensorReadingQueryPort
) : QuerySensorReadingsUseCase {

    override fun getLatestReadings(limit: Int): List<SensorReading> {
        return queryPort.findTopNOrderByTimeDesc(limit)
    }

    override fun getReadingsByCode(code: String, start: Instant, end: Instant): List<SensorReading> {
        return queryPort.findByCodeAndTimeBetween(code, start, end)
    }

    override fun getCurrentValues(): Map<String, Any?> {
        val readings = queryPort.findLatestForAllCodes()
        val currentValues = readings.associate { reading ->
            reading.code to mapOf(
                "value" to reading.value,
                "timestamp" to reading.time
            )
        }
        return mapOf(
            "sensors" to currentValues,
            "timestamp" to Instant.now()
        )
    }
}

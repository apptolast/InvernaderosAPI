package com.apptolast.invernaderos.features.sensor

import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import java.time.Instant
import org.springframework.stereotype.Service

@Service
class SensorReadingService(private val sensorReadingRepository: SensorReadingRepository) {

    fun getLatestReadings(limit: Int): List<SensorReadingResponse> {
        val readings = sensorReadingRepository.findTopNOrderByTimeDesc(limit)
        return readings.map { it.toResponse() }
    }

    fun getReadingsByCode(
            code: String,
            start: Instant,
            end: Instant
    ): List<SensorReadingResponse> {
        val readings = sensorReadingRepository.findByCodeAndTimeBetween(code, start, end)
        return readings.map { it.toResponse() }
    }

    fun getCurrentValues(): Map<String, Any?> {
        val readings = sensorReadingRepository.findLatestForAllCodes()
        val currentValues =
                readings.associate { reading ->
                    reading.code to
                            mapOf(
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

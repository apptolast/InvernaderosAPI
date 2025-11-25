package com.apptolast.invernaderos.features.sensor

import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import java.time.Instant
import org.springframework.stereotype.Service

@Service
class SensorReadingService(private val sensorReadingRepository: SensorReadingRepository) {

    fun getLatestReadings(greenhouseId: String?, limit: Int): List<SensorReadingResponse> {
        val readings =
                if (greenhouseId != null) {
                    sensorReadingRepository.findTopNByGreenhouseIdOrderByTimeDesc(
                            greenhouseId,
                            limit
                    )
                } else {
                    sensorReadingRepository.findTopNOrderByTimeDesc(limit)
                }
        return readings.map { it.toResponse() }
    }

    fun getReadingsByGreenhouse(greenhouseId: String, since: Instant): List<SensorReadingResponse> {
        val readings = sensorReadingRepository.findByGreenhouseIdSince(greenhouseId, since)
        return readings.map { it.toResponse() }
    }

    fun getReadingsBySensor(
            sensorId: String,
            start: Instant,
            end: Instant
    ): List<SensorReadingResponse> {
        val readings = sensorReadingRepository.findBySensorIdAndTimeBetween(sensorId, start, end)
        return readings.map { it.toResponse() }
    }

    fun getCurrentSensorValues(greenhouseId: String): Map<String, Any?> {
        val readings = sensorReadingRepository.findLatestBySensorForGreenhouse(greenhouseId)
        val currentValues =
                readings.associate { reading ->
                    reading.sensorId to
                            mapOf(
                                    "value" to reading.value,
                                    "unit" to reading.unit,
                                    "timestamp" to reading.time,
                                    "type" to reading.sensorType
                            )
                }
        return mapOf(
                "greenhouseId" to greenhouseId,
                "sensors" to currentValues,
                "timestamp" to Instant.now()
        )
    }

    fun getSensorStats(sensorId: String, start: Instant, end: Instant): Map<String, Any?> {
        val readings = sensorReadingRepository.findBySensorIdAndTimeBetween(sensorId, start, end)

        if (readings.isEmpty()) {
            return mapOf("message" to "No data found")
        }

        val values = readings.map { it.value }
        return mapOf(
                "sensorId" to sensorId,
                "min" to values.minOrNull(),
                "max" to values.maxOrNull(),
                "avg" to values.average(),
                "count" to values.size,
                "period" to mapOf("start" to start, "end" to end)
        )
    }

    fun getSensorTrend(
            sensorId: String,
            tenantId: String?,
            start: Instant,
            end: Instant,
            period: String
    ): SensorTrendDto? {
        val trendData =
                sensorReadingRepository.calculateTrend(
                        sensorId = sensorId,
                        tenantId = tenantId,
                        startTime = start,
                        endTime = end
                )
                        ?: return null

        val firstValue = (trendData["first_value"] as? Number)?.toDouble() ?: return null
        val lastValue = (trendData["last_value"] as? Number)?.toDouble() ?: return null
        val firstTime = trendData["first_time"] as? Instant ?: start
        val lastTime = trendData["last_time"] as? Instant ?: end
        val unit = trendData["unit"] as? String

        val absoluteChange = lastValue - firstValue
        val percentageChange =
                if (firstValue != 0.0) {
                    ((lastValue - firstValue) / firstValue) * 100
                } else {
                    0.0
                }
        val direction = SensorTrendDto.calculateDirection(percentageChange)

        return SensorTrendDto(
                sensorId = sensorId,
                currentValue = lastValue,
                previousValue = firstValue,
                percentageChange = percentageChange,
                absoluteChange = absoluteChange,
                direction = direction,
                period = period,
                currentTimestamp = lastTime,
                previousTimestamp = firstTime,
                unit = unit
        )
    }
}

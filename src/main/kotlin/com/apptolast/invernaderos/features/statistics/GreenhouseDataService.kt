package com.apptolast.invernaderos.features.statistics

import com.apptolast.invernaderos.features.statistics.GreenhouseStatisticsDto
import com.apptolast.invernaderos.features.statistics.GreenhouseSummaryDto
import com.apptolast.invernaderos.features.statistics.SensorSummary
import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Servicio de logica de negocio para estadisticas de datos del invernadero.
 * Consulta TimescaleDB para calcular agregaciones sobre sensor_readings.
 */
@Service
class GreenhouseDataService(
        private val sensorReadingRepository: SensorReadingRepository
) {

    private val logger = LoggerFactory.getLogger(GreenhouseDataService::class.java)

    /**
     * Obtiene estadisticas de un sensor especifico en un periodo.
     */
    fun getSensorStatistics(sensorId: String, period: String = "1h"): GreenhouseStatisticsDto? {
        logger.debug("Obteniendo estadisticas de {} para periodo {}", sensorId, period)

        val (startTime, endTime) = parsePeriod(period)
        val sensorType = determineSensorType(sensorId)

        val readings = sensorReadingRepository.findByCodeAndTimeBetween(sensorId, startTime, endTime)

        return if (readings.isNotEmpty()) {
            val values = readings.mapNotNull { it.value.toDoubleOrNull() }
            GreenhouseStatisticsDto(
                    sensorId = sensorId,
                    sensorType = sensorType,
                    min = values.minOrNull(),
                    max = values.maxOrNull(),
                    avg = if (values.isNotEmpty()) values.average() else null,
                    count = readings.size.toLong(),
                    lastValue = values.firstOrNull(),
                    lastTimestamp = readings.first().time,
                    periodStart = startTime,
                    periodEnd = endTime
            )
        } else {
            null
        }
    }

    /**
     * Obtiene un resumen de estadisticas de todos los sensores y setpoints.
     */
    fun getSummaryStatistics(period: String = "1h"): GreenhouseSummaryDto {
        logger.debug("Obteniendo resumen de estadisticas para periodo {}", period)

        val (startTime, endTime) = parsePeriod(period)
        val allSensors = sensorReadingRepository.findDistinctCodes()

        val sensors = mutableMapOf<String, SensorSummary>()
        val setpoints = mutableMapOf<String, SensorSummary>()

        allSensors.forEach { code ->
            val readings = sensorReadingRepository.findByCodeAndTimeBetween(code, startTime, endTime)

            if (readings.isNotEmpty()) {
                val values = readings.mapNotNull { it.value.toDoubleOrNull() }
                val summary =
                        SensorSummary(
                                current = values.firstOrNull(),
                                min = values.minOrNull(),
                                max = values.maxOrNull(),
                                avg = if (values.isNotEmpty()) values.average() else null,
                                count = readings.size.toLong()
                        )

                when {
                    code.startsWith("SET-") -> setpoints[code] = summary
                    code.startsWith("DEV-") -> sensors[code] = summary
                }
            }
        }

        val totalMessages = sensorReadingRepository.countByTimeBetween(startTime, endTime)

        return GreenhouseSummaryDto(
                timestamp = Instant.now(),
                totalMessages = totalMessages,
                sensors = sensors,
                setpoints = setpoints,
                periodStart = startTime,
                periodEnd = endTime
        )
    }

    private fun parsePeriod(period: String): Pair<Instant, Instant> {
        val endTime = Instant.now()
        val startTime =
                when {
                    period.endsWith("h") -> {
                        val hours = period.removeSuffix("h").toLongOrNull() ?: 1
                        endTime.minus(hours, ChronoUnit.HOURS)
                    }
                    period.endsWith("d") -> {
                        val days = period.removeSuffix("d").toLongOrNull() ?: 1
                        endTime.minus(days, ChronoUnit.DAYS)
                    }
                    period.endsWith("m") -> {
                        val minutes = period.removeSuffix("m").toLongOrNull() ?: 60
                        endTime.minus(minutes, ChronoUnit.MINUTES)
                    }
                    else -> endTime.minus(1, ChronoUnit.HOURS)
                }
        return Pair(startTime, endTime)
    }

    private fun determineSensorType(sensorId: String): String {
        return when {
            sensorId.startsWith("DEV-") -> "DEVICE"
            sensorId.startsWith("SET-") -> "SETTING"
            else -> "UNKNOWN"
        }
    }
}

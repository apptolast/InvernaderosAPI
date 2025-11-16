package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.dtos.SensorTrendDto
import com.apptolast.invernaderos.entities.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.repositories.timeseries.SensorReadingRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit

@Suppress("UNCHECKED_CAST")
@RestController
@RequestMapping("/api/sensors")
class SensorReadingController(
    private val sensorReadingRepository: SensorReadingRepository
) {

    /**
     * GET /api/sensors/latest
     * Obtiene las últimas lecturas de sensores
     * Optimizado: usa query específica en lugar de findAll().filter()
     */
    @GetMapping("/latest")
    fun getLatestReadings(
        @RequestParam(required = false) greenhouseId: String? = "001",
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<SensorReading>> {
        val readings = if (greenhouseId != null) {
            sensorReadingRepository.findTopNByGreenhouseIdOrderByTimeDesc(greenhouseId, limit)
        } else {
            sensorReadingRepository.findTopNOrderByTimeDesc(limit)
        }

        return ResponseEntity.ok(readings)
    }

    /**
     * GET /api/sensors/by-greenhouse/{greenhouseId}
     * Obtiene lecturas de un greenhouse específico en las últimas 24 horas
     */
    @GetMapping("/by-greenhouse/{greenhouseId}")
    fun getReadingsByGreenhouse(
        @PathVariable greenhouseId: String,
        @RequestParam(required = false) hours: Long = 24
    ): ResponseEntity<List<SensorReading>> {
        val since = Instant.now().minus(hours, ChronoUnit.HOURS)
        val readings = sensorReadingRepository.findByGreenhouseIdSince(greenhouseId, since)

        return ResponseEntity.ok(readings)
    }

    /**
     * GET /api/sensors/by-sensor/{sensorId}
     * Obtiene lecturas de un sensor específico en un rango de tiempo
     */
    @GetMapping("/by-sensor/{sensorId}")
    fun getReadingsBySensor(
        @PathVariable sensorId: String,
        @RequestParam(required = false) hoursAgo: Long = 24
    ): ResponseEntity<List<SensorReading>> {
        val end = Instant.now()
        val start = end.minus(hoursAgo, ChronoUnit.HOURS)

        val readings = sensorReadingRepository.findBySensorIdAndTimeBetween(
            sensorId = sensorId,
            start = start,
            end = end
        )

        return ResponseEntity.ok(readings)
    }

    /**
     * GET /api/sensors/current
     * Obtiene el estado actual de todos los sensores (última lectura de cada uno)
     * Optimizado: usa query DISTINCT ON en lugar de findAll().filter().groupBy()
     */
    @GetMapping("/current")
    fun getCurrentSensorValues(
        @RequestParam(required = false) greenhouseId: String? = "001"
    ): ResponseEntity<Map<String, Any>> {
        if (greenhouseId == null) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "greenhouseId is required")
            ) as ResponseEntity<Map<String, Any>>
        }

        // Optimizado: obtiene directamente la última lectura de cada sensor
        val readings = sensorReadingRepository.findLatestBySensorForGreenhouse(greenhouseId)

        val currentValues = readings.associate { reading ->
            reading.sensorId to mapOf(
                "value" to reading.value,
                "unit" to reading.unit,
                "timestamp" to reading.time,
                "type" to reading.sensorType
            )
        }

        return ResponseEntity.ok(mapOf(
            "greenhouseId" to greenhouseId,
            "sensors" to currentValues,
            "timestamp" to Instant.now()
        ))
    }

    /**
     * GET /api/sensors/stats/{sensorId}
     * Obtiene estadísticas de un sensor (min, max, avg)
     */
    @GetMapping("/stats/{sensorId}")
    fun getSensorStats(
        @PathVariable sensorId: String,
        @RequestParam(required = false) hoursAgo: Long = 24
    ): ResponseEntity<Map<String, Any>> {
        val end = Instant.now()
        val start = end.minus(hoursAgo, ChronoUnit.HOURS)

        val readings = sensorReadingRepository.findBySensorIdAndTimeBetween(
            sensorId = sensorId,
            start = start,
            end = end
        )

        if (readings.isEmpty()) {
            return ResponseEntity.ok(mapOf("message" to "No data found"))
        }

        val values = readings.map { it.value }
        val stats = mapOf(
            "sensorId" to sensorId,
            "min" to values.minOrNull(),
            "max" to values.maxOrNull(),
            "avg" to values.average(),
            "count" to values.size,
            "period" to mapOf(
                "start" to start,
                "end" to end
            )
        )

        return ResponseEntity.ok(stats) as ResponseEntity<Map<String, Any>>
    }

    /**
     * GET /api/sensors/stats/{sensorId}/trend
     * Calcula la tendencia (porcentaje de cambio) de un sensor en un periodo
     *
     * Usado por: Pantalla "Historial de Datos" - indicador de tendencia (+1.2%)
     *
     * @param sensorId ID del sensor (e.g., "TEMPERATURA INVERNADERO 01")
     * @param tenantId ID del tenant (null = DEFAULT para backward compatibility)
     * @param period Periodo para calcular tendencia: "1h", "24h", "7d", "30d" (default: 24h)
     * @return SensorTrendDto con percentageChange, direction, etc.
     */
    @GetMapping("/stats/{sensorId}/trend")
    fun getSensorTrend(
        @PathVariable sensorId: String,
        @RequestParam(required = false) tenantId: String?,
        @RequestParam(defaultValue = "24h") period: String
    ): ResponseEntity<SensorTrendDto> {
        val end = Instant.now()
        val start = parsePeriod(period, end)

        return try {
            val trendData = sensorReadingRepository.calculateTrend(
                sensorId = sensorId,
                tenantId = tenantId,
                startTime = start,
                endTime = end
            )

            if (trendData == null) {
                return ResponseEntity.notFound().build()
            }

            // Extract values from query result
            val firstValue = (trendData["first_value"] as? Number)?.toDouble() ?: return ResponseEntity.notFound().build()
            val lastValue = (trendData["last_value"] as? Number)?.toDouble() ?: return ResponseEntity.notFound().build()
            val firstTime = trendData["first_time"] as? Instant ?: start
            val lastTime = trendData["last_time"] as? Instant ?: end
            val unit = trendData["unit"] as? String

            // Calculate trend metrics
            val absoluteChange = lastValue - firstValue
            val percentageChange = if (firstValue != 0.0) {
                ((lastValue - firstValue) / firstValue) * 100
            } else {
                0.0
            }
            val direction = SensorTrendDto.calculateDirection(percentageChange)

            val trend = SensorTrendDto(
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

            ResponseEntity.ok(trend)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * Parsea un string de periodo a timestamp de inicio
     */
    private fun parsePeriod(period: String, end: Instant): Instant {
        return when {
            period.endsWith("h") -> {
                val hours = period.removeSuffix("h").toLongOrNull() ?: 24
                end.minus(hours, ChronoUnit.HOURS)
            }
            period.endsWith("d") -> {
                val days = period.removeSuffix("d").toLongOrNull() ?: 1
                end.minus(days, ChronoUnit.DAYS)
            }
            period.endsWith("m") -> {
                val minutes = period.removeSuffix("m").toLongOrNull() ?: 60
                end.minus(minutes, ChronoUnit.MINUTES)
            }
            else -> end.minus(24, ChronoUnit.HOURS)
        }
    }
}
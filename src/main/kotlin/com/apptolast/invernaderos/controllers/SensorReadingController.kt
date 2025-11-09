package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.repositories.timeseries.SensorReadingRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit

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
        )) as ResponseEntity<Map<String, Any>>
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
}
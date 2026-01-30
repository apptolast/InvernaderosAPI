package com.apptolast.invernaderos.features.sensor

import io.swagger.v3.oas.annotations.Operation
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Suppress("UNCHECKED_CAST")
@RestController
@RequestMapping("/api/v1/sensors")
class SensorReadingController(private val sensorReadingService: SensorReadingService) {

        /** GET /api/sensors/latest Obtiene las últimas lecturas de sensores */
        @GetMapping("/latest")
        @Operation(summary = "Get latest sensor readings")
        fun getLatestReadings(
                @RequestParam(required = false) greenhouseId: String? = "001",
                @RequestParam(defaultValue = "10") limit: Int
        ): ResponseEntity<List<SensorReadingResponse>> {
                return ResponseEntity.ok(
                        sensorReadingService.getLatestReadings(greenhouseId, limit)
                )
        }

        /**
         * GET /api/sensors/by-greenhouse/{greenhouseId} Obtiene lecturas de un greenhouse
         * específico en las últimas 24 horas
         */
        @GetMapping("/by-greenhouse/{greenhouseId}")
        @Operation(summary = "Get readings by greenhouse")
        fun getReadingsByGreenhouse(
                @PathVariable greenhouseId: String,
                @RequestParam(required = false) hours: Long = 24
        ): ResponseEntity<List<SensorReadingResponse>> {
                val since = Instant.now().minus(hours, ChronoUnit.HOURS)
                return ResponseEntity.ok(
                        sensorReadingService.getReadingsByGreenhouse(greenhouseId, since)
                )
        }

        /**
         * GET /api/sensors/by-sensor/{sensorId} Obtiene lecturas de un sensor específico en un
         * rango de tiempo
         */
        @GetMapping("/by-sensor/{sensorId}")
        @Operation(summary = "Get readings by sensor ID")
        fun getReadingsBySensor(
                @PathVariable sensorId: String,
                @RequestParam(required = false) hoursAgo: Long = 24
        ): ResponseEntity<List<SensorReadingResponse>> {
                val end = Instant.now()
                val start = end.minus(hoursAgo, ChronoUnit.HOURS)
                return ResponseEntity.ok(
                        sensorReadingService.getReadingsBySensor(sensorId, start, end)
                )
        }

        /**
         * GET /api/sensors/current Obtiene el estado actual de todos los sensores (última lectura
         * de cada uno)
         */
        @GetMapping("/current")
        @Operation(summary = "Get current values for all sensors in a greenhouse")
        fun getCurrentSensorValues(
                @RequestParam(required = true) greenhouseId: Long
        ): ResponseEntity<Map<String, Any?>> {
                return ResponseEntity.ok(sensorReadingService.getCurrentSensorValues(greenhouseId))
        }

        /** GET /api/sensors/stats/{sensorId} Obtiene estadísticas de un sensor (min, max, avg) */
        @GetMapping("/stats/{sensorId}")
        @Operation(summary = "Get statistics for a sensor")
        fun getSensorStats(
                @PathVariable sensorId: String,
                @RequestParam(required = false) hoursAgo: Long = 24
        ): ResponseEntity<Map<String, Any?>> {
                val end = Instant.now()
                val start = end.minus(hoursAgo, ChronoUnit.HOURS)
                return ResponseEntity.ok(sensorReadingService.getSensorStats(sensorId, start, end))
        }

        /**
         * GET /api/sensors/stats/{sensorId}/trend Calcula la tendencia (porcentaje de cambio) de un
         * sensor en un periodo
         */
        @GetMapping("/stats/{sensorId}/trend")
        @Operation(summary = "Get sensor trend")
        fun getSensorTrend(
                @PathVariable sensorId: String,
                @RequestParam(required = false) tenantId: String?,
                @RequestParam(defaultValue = "24h") period: String
        ): ResponseEntity<SensorTrendDto> {
                val end = Instant.now()
                val start = parsePeriod(period, end)

                return try {
                        val trend =
                                sensorReadingService.getSensorTrend(
                                        sensorId,
                                        tenantId,
                                        start,
                                        end,
                                        period
                                )
                        if (trend != null) {
                                ResponseEntity.ok(trend)
                        } else {
                                ResponseEntity.notFound().build()
                        }
                } catch (e: Exception) {
                        ResponseEntity.internalServerError().build()
                }
        }

        /** Parsea un string de periodo a timestamp de inicio */
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

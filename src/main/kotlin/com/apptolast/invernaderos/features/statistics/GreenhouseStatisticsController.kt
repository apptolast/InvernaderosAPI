package com.apptolast.invernaderos.features.statistics

import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para estadisticas de datos del invernadero.
 *
 * Endpoints:
 * - GET /api/v1/greenhouse/statistics/{sensorId} - Estadisticas de un sensor
 * - GET /api/v1/greenhouse/statistics/summary - Resumen de todos los sensores
 * - GET /api/v1/greenhouse/health - Health check
 */
@RestController
@RequestMapping("/api/v1/greenhouse")
@CrossOrigin(origins = ["*"])
class GreenhouseStatisticsController(private val greenhouseDataService: GreenhouseDataService) {

    private val logger = LoggerFactory.getLogger(GreenhouseStatisticsController::class.java)

    @GetMapping("/statistics/{sensorId}")
    fun getSensorStatistics(
            @PathVariable sensorId: String,
            @RequestParam(defaultValue = "1h") period: String
    ): ResponseEntity<GreenhouseStatisticsDto> {
        logger.debug("GET /api/v1/greenhouse/statistics/{}?period={}", sensorId, period)

        return try {
            val stats = greenhouseDataService.getSensorStatistics(sensorId, period)
            if (stats != null) {
                ResponseEntity.ok(stats)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error obteniendo estadisticas del sensor {}", sensorId, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/statistics/summary")
    fun getSummaryStatistics(
            @RequestParam(defaultValue = "1h") period: String
    ): ResponseEntity<GreenhouseSummaryDto> {
        logger.debug("GET /api/v1/greenhouse/statistics/summary?period={}", period)

        return try {
            val summary = greenhouseDataService.getSummaryStatistics(period)
            ResponseEntity.ok(summary)
        } catch (e: Exception) {
            logger.error("Error obteniendo resumen de estadisticas", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
                mapOf(
                        "status" to "UP",
                        "service" to "Greenhouse IoT API",
                        "timestamp" to Instant.now().toString()
                )
        )
    }
}

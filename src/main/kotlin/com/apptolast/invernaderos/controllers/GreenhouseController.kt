package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.dtos.GreenhouseStatisticsDto
import com.apptolast.invernaderos.entities.dtos.GreenhouseSummaryDto
import com.apptolast.invernaderos.entities.dtos.RealDataDto
import com.apptolast.invernaderos.service.GreenhouseDataService
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * REST Controller para gestionar datos del invernadero recibidos via MQTT
 *
 * Endpoints disponibles:
 * - GET /api/greenhouse/messages/recent - Últimos N mensajes
 * - GET /api/greenhouse/messages/range - Mensajes por rango de tiempo
 * - GET /api/greenhouse/messages/latest - Último mensaje recibido
 * - GET /api/greenhouse/statistics/{sensorId} - Estadísticas de un sensor
 * - GET /api/greenhouse/statistics/summary - Resumen de todos los sensores
 * - GET /api/greenhouse/cache/info - Información del caché Redis
 */
@RestController
@RequestMapping("/api/greenhouse")
@CrossOrigin(origins = ["*"]) // Permite CORS para desarrollo, ajustar en producción
class GreenhouseController(
    private val greenhouseDataService: GreenhouseDataService
) {

    private val logger = LoggerFactory.getLogger(GreenhouseController::class.java)

    /**
     * Obtiene los últimos N mensajes recibidos del topic GREENHOUSE
     *
     * GET /api/greenhouse/messages/recent?limit=100
     *
     * @param limit Número de mensajes a obtener (default: 100, max: 1000)
     * @return Lista de mensajes ordenados por timestamp descendente
     */
    @GetMapping("/messages/recent")
    fun getRecentMessages(
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<RealDataDto>> {
        logger.debug("GET /api/greenhouse/messages/recent?limit={}", limit)

        // Validar límite
        val validatedLimit = limit.coerceIn(1, 1000)

        return try {
            val messages = greenhouseDataService.getRecentMessages(validatedLimit)
            ResponseEntity.ok(messages)
        } catch (e: Exception) {
            logger.error("Error obteniendo mensajes recientes", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(emptyList())
        }
    }

    /**
     * Obtiene mensajes en un rango de tiempo específico
     *
     * GET /api/greenhouse/messages/range?from=2025-11-09T10:00:00Z&to=2025-11-09T11:00:00Z
     *
     * @param from Timestamp de inicio (formato ISO-8601)
     * @param to Timestamp de fin (formato ISO-8601)
     * @return Lista de mensajes en el rango especificado
     */
    @GetMapping("/messages/range")
    fun getMessagesByTimeRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant
    ): ResponseEntity<List<RealDataDto>> {
        logger.debug("GET /api/greenhouse/messages/range?from={}&to={}", from, to)

        return try {
            // Validar que 'from' sea anterior a 'to'
            if (from.isAfter(to)) {
                return ResponseEntity.badRequest().build()
            }

            val messages = greenhouseDataService.getMessagesByTimeRange(from, to)
            ResponseEntity.ok(messages)
        } catch (e: Exception) {
            logger.error("Error obteniendo mensajes por rango de tiempo", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(emptyList())
        }
    }

    /**
     * Obtiene el último mensaje recibido
     *
     * GET /api/greenhouse/messages/latest
     *
     * @return El mensaje más reciente o 404 si no hay mensajes
     */
    @GetMapping("/messages/latest")
    fun getLatestMessage(): ResponseEntity<RealDataDto> {
        logger.debug("GET /api/greenhouse/messages/latest")

        return try {
            val message = greenhouseDataService.getLatestMessage()
            if (message != null) {
                ResponseEntity.ok(message)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error obteniendo último mensaje", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Obtiene estadísticas de un sensor o setpoint específico
     *
     * GET /api/greenhouse/statistics/SENSOR_01?period=1h
     *
     * @param sensorId ID del sensor (ej: SENSOR_01, SETPOINT_01)
     * @param period Periodo para calcular estadísticas (1h, 24h, 7d, 30d)
     * @return Estadísticas del sensor (min, max, avg, count, lastValue)
     */
    @GetMapping("/statistics/{sensorId}")
    fun getSensorStatistics(
        @PathVariable sensorId: String,
        @RequestParam(defaultValue = "1h") period: String
    ): ResponseEntity<GreenhouseStatisticsDto> {
        logger.debug("GET /api/greenhouse/statistics/{}?period={}", sensorId, period)

        return try {
            val stats = greenhouseDataService.getSensorStatistics(sensorId, period)
            if (stats != null) {
                ResponseEntity.ok(stats)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error obteniendo estadísticas del sensor {}", sensorId, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Obtiene un resumen de estadísticas de todos los sensores y setpoints
     *
     * GET /api/greenhouse/statistics/summary?period=1h
     *
     * @param period Periodo para calcular estadísticas (1h, 24h, 7d, 30d)
     * @return Resumen completo con estadísticas de todos los sensores y setpoints
     */
    @GetMapping("/statistics/summary")
    fun getSummaryStatistics(
        @RequestParam(defaultValue = "1h") period: String
    ): ResponseEntity<GreenhouseSummaryDto> {
        logger.debug("GET /api/greenhouse/statistics/summary?period={}", period)

        return try {
            val summary = greenhouseDataService.getSummaryStatistics(period)
            ResponseEntity.ok(summary)
        } catch (e: Exception) {
            logger.error("Error obteniendo resumen de estadísticas", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Obtiene información sobre el estado del caché Redis
     *
     * GET /api/greenhouse/cache/info
     *
     * @return Información del caché (total mensajes, TTL, capacidad, etc.)
     */
    @GetMapping("/cache/info")
    fun getCacheInfo(): ResponseEntity<Map<String, Any>> {
        logger.debug("GET /api/greenhouse/cache/info")

        return try {
            val info = greenhouseDataService.getCacheInfo()
            ResponseEntity.ok(info)
        } catch (e: Exception) {
            logger.error("Error obteniendo información del caché", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(emptyMap())
        }
    }

    /**
     * Health check endpoint
     *
     * GET /api/greenhouse/health
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "Greenhouse MQTT API",
                "timestamp" to Instant.now().toString()
            )
        )
    }
}

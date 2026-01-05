package com.apptolast.invernaderos.features.statistics

import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.HistoricalDataDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsDailyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsHourlyDto
import com.apptolast.invernaderos.features.statistics.StatisticsService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST Controller para estadísticas agregadas.
 *
 * Endpoints para la pantalla "Historial de Datos" del frontend Kotlin Multiplatform:
 * - GET /api/statistics/historical-data - Datos completos (valor actual + trend + min/max/avg + gráfica)
 * - GET /api/statistics/hourly - Estadísticas horarias (últimas Nh)
 * - GET /api/statistics/daily - Estadísticas diarias (últimos Nd)
 * - GET /api/statistics/summary - Solo resumen (min/max/avg)
 */
@RestController
@RequestMapping("/api/v1/statistics")
@CrossOrigin(origins = ["*"])
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * GET /api/statistics/historical-data?greenhouseId=xxx&sensorType=TEMPERATURE&period=24h
     *
     * Endpoint principal para la pantalla "Historial de Datos".
     *
     * Query params:
     * - greenhouseId: UUID del invernadero (requerido)
     * - sensorType: TEMPERATURE, HUMIDITY, CO2, LIGHT, etc. (requerido)
     * - period: 24h | 7d | 30d (opcional, default: 24h)
     *
     * Response: HistoricalDataDto
     * {
     *   "currentValue": 22.5,
     *   "currentValueTimestamp": "2025-01-15T14:30:00Z",
     *   "avgValue": 21.8,
     *   "minValue": 19.5,
     *   "maxValue": 24.1,
     *   "trendPercent": 1.2,
     *   "trendDirection": "INCREASING",
     *   "chartData": [
     *     {"timestamp": "2025-01-15T00:00:00Z", "value": 20.5},
     *     {"timestamp": "2025-01-15T01:00:00Z", "value": 20.8},
     *     ...
     *   ],
     *   "period": "24h",
     *   "unit": "°C"
     * }
     */
    @GetMapping("/historical-data")
    fun getHistoricalData(
        @RequestParam greenhouseId: UUID,
        @RequestParam sensorType: String,
        @RequestParam(required = false, defaultValue = "24h") period: String
    ): ResponseEntity<HistoricalDataDto> {
        logger.debug("GET /api/statistics/historical-data?greenhouseId=$greenhouseId&sensorType=$sensorType&period=$period")

        return try {
            val data = statisticsService.getHistoricalData(greenhouseId, sensorType, period)
            if (data != null) {
                ResponseEntity.ok(data)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error getting historical data", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/statistics/hourly?greenhouseId=xxx&sensorType=TEMPERATURE&hours=24
     *
     * Obtiene estadísticas horarias (para gráficas de corto plazo).
     *
     * Query params:
     * - greenhouseId: UUID del invernadero
     * - sensorType: TEMPERATURE, HUMIDITY, etc.
     * - hours: Número de horas (default: 24)
     *
     * Response: List<SensorStatisticsHourlyDto>
     */
    @GetMapping("/hourly")
    fun getHourlyStatistics(
        @RequestParam greenhouseId: UUID,
        @RequestParam sensorType: String,
        @RequestParam(required = false, defaultValue = "24") hours: Int
    ): ResponseEntity<List<SensorStatisticsHourlyDto>> {
        logger.debug("GET /api/statistics/hourly?greenhouseId=$greenhouseId&sensorType=$sensorType&hours=$hours")

        return try {
            val stats = statisticsService.getHourlyStatistics(greenhouseId, sensorType, hours)
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            logger.error("Error getting hourly statistics", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/statistics/daily?greenhouseId=xxx&sensorType=TEMPERATURE&days=7
     *
     * Obtiene estadísticas diarias (para gráficas de mediano/largo plazo).
     *
     * Query params:
     * - greenhouseId: UUID del invernadero
     * - sensorType: TEMPERATURE, HUMIDITY, etc.
     * - days: Número de días (default: 7)
     *
     * Response: List<SensorStatisticsDailyDto>
     */
    @GetMapping("/daily")
    fun getDailyStatistics(
        @RequestParam greenhouseId: UUID,
        @RequestParam sensorType: String,
        @RequestParam(required = false, defaultValue = "7") days: Int
    ): ResponseEntity<List<SensorStatisticsDailyDto>> {
        logger.debug("GET /api/statistics/daily?greenhouseId=$greenhouseId&sensorType=$sensorType&days=$days")

        return try {
            val stats = statisticsService.getDailyStatistics(greenhouseId, sensorType, days)
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            logger.error("Error getting daily statistics", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/statistics/summary?greenhouseId=xxx&sensorType=TEMPERATURE&period=24h
     *
     * Obtiene solo resumen estadístico (sin datos de gráfica).
     *
     * Query params:
     * - greenhouseId: UUID del invernadero
     * - sensorType: TEMPERATURE, HUMIDITY, etc.
     * - period: 24h | 7d | 30d (default: 24h)
     *
     * Response: Map<String, Any>
     * {
     *   "overall_avg": 21.8,
     *   "overall_min": 19.5,
     *   "overall_max": 24.1,
     *   "unit": "°C",
     *   "total_readings": 1440
     * }
     */
    @GetMapping("/summary")
    fun getStatisticsSummary(
        @RequestParam greenhouseId: UUID,
        @RequestParam sensorType: String,
        @RequestParam(required = false, defaultValue = "24h") period: String
    ): ResponseEntity<Map<String, Any?>> {
        logger.debug("GET /api/statistics/summary?greenhouseId=$greenhouseId&sensorType=$sensorType&period=$period")

        return try {
            val summary = statisticsService.getStatisticsSummary(greenhouseId, sensorType, period)
            ResponseEntity.ok(summary)
        } catch (e: Exception) {
            logger.error("Error getting statistics summary", e)
            ResponseEntity.internalServerError().build()
        }
    }
}

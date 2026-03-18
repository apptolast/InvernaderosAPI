package com.apptolast.invernaderos.features.statistics

import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.HistoricalDataDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsDailyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsHourlyDto
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller para estadisticas agregadas.
 *
 * Todos los endpoints reciben un code como parametro principal.
 * El code es la clave de cruce con PostgreSQL metadata.
 * Los metadatos de negocio (unit, sensor_type, greenhouse) se resuelven
 * en la capa de servicio, no en TimescaleDB.
 *
 * Endpoints:
 * - GET /api/v1/statistics/historical-data?code=DEV-00038&period=24h
 * - GET /api/v1/statistics/hourly?code=DEV-00038&hours=24
 * - GET /api/v1/statistics/daily?code=DEV-00038&days=7
 * - GET /api/v1/statistics/summary?code=DEV-00038&period=24h
 */
@RestController
@RequestMapping("/api/v1/statistics")
@CrossOrigin(origins = ["*"])
class StatisticsController(
    private val statisticsService: StatisticsService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * GET /api/v1/statistics/historical-data?code=DEV-00038&period=24h
     *
     * Datos completos para la pantalla "Historial de Datos":
     * valor actual, promedio, min, max, trend, grafica.
     */
    @GetMapping("/historical-data")
    fun getHistoricalData(
        @RequestParam code: String,
        @RequestParam(required = false, defaultValue = "24h") period: String
    ): ResponseEntity<HistoricalDataDto> {
        logger.debug("GET /api/v1/statistics/historical-data?code={}&period={}", code, period)

        return try {
            val data = statisticsService.getHistoricalData(code, period)
            if (data != null) {
                ResponseEntity.ok(data)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error getting historical data for code={}", code, e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/v1/statistics/hourly?code=DEV-00038&hours=24
     *
     * Estadisticas horarias para graficas de corto plazo.
     */
    @GetMapping("/hourly")
    fun getHourlyStatistics(
        @RequestParam code: String,
        @RequestParam(required = false, defaultValue = "24") hours: Int
    ): ResponseEntity<List<SensorStatisticsHourlyDto>> {
        logger.debug("GET /api/v1/statistics/hourly?code={}&hours={}", code, hours)

        return try {
            val stats = statisticsService.getHourlyStatistics(code, hours)
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            logger.error("Error getting hourly statistics for code={}", code, e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/v1/statistics/daily?code=DEV-00038&days=7
     *
     * Estadisticas diarias para graficas de mediano/largo plazo.
     */
    @GetMapping("/daily")
    fun getDailyStatistics(
        @RequestParam code: String,
        @RequestParam(required = false, defaultValue = "7") days: Int
    ): ResponseEntity<List<SensorStatisticsDailyDto>> {
        logger.debug("GET /api/v1/statistics/daily?code={}&days={}", code, days)

        return try {
            val stats = statisticsService.getDailyStatistics(code, days)
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            logger.error("Error getting daily statistics for code={}", code, e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/v1/statistics/summary?code=DEV-00038&period=24h
     *
     * Solo resumen estadistico (min/max/avg) sin datos de grafica.
     */
    @GetMapping("/summary")
    fun getStatisticsSummary(
        @RequestParam code: String,
        @RequestParam(required = false, defaultValue = "24h") period: String
    ): ResponseEntity<Map<String, Any?>> {
        logger.debug("GET /api/v1/statistics/summary?code={}&period={}", code, period)

        return try {
            val summary = statisticsService.getStatisticsSummary(code, period)
            ResponseEntity.ok(summary)
        } catch (e: Exception) {
            logger.error("Error getting statistics summary for code={}", code, e)
            ResponseEntity.internalServerError().build()
        }
    }
}

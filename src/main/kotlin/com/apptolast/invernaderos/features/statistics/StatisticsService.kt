package com.apptolast.invernaderos.features.statistics

import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import com.apptolast.invernaderos.features.statistics.dao.StatsDao
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.ChartDataPoint
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.HistoricalDataDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsDailyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsHourlyDto
import java.time.Instant
import kotlin.math.abs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * Service para consultas de estadísticas agregadas.
 *
 * Calcula métricas para la pantalla "Historial de Datos":
 * - Promedio, Mín, Máx
 * - Trend percentage (↑ +1.2%, ↓ -0.5%)
 * - Datos para gráficas (time-series)
 */
@Service
class StatisticsService(
        @Qualifier("statsDaoBean") private val statisticsDao: StatsDao,
        private val greenhouseRepository: GreenhouseRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * GET /api/statistics/historical-data?greenhouseId=xxx&sensorType=TEMPERATURE&period=24h
     *
     * Obtiene datos completos para la pantalla "Historial de Datos".
     */
    fun getHistoricalData(
            greenhouseId: Long,
            sensorType: String,
            period: String = "24h" // "24h", "7d", "30d"
    ): HistoricalDataDto? {
        logger.debug(
                "Getting historical data for greenhouse=$greenhouseId, sensor=$sensorType, period=$period"
        )

        val (hours, days, aggregationType) = parsePeriod(period)

        // 1. Obtener greenhouse para obtener tenantId
        val greenhouse = greenhouseRepository.findById(greenhouseId).orElse(null)
        if (greenhouse == null) {
            logger.warn("Greenhouse not found: $greenhouseId")
            return null
        }

        // 2. Obtener valor actual (última lectura)
        val latest = statisticsDao.getLatestValue(greenhouseId, sensorType)
        if (latest == null) {
            logger.warn("No data found for greenhouse=$greenhouseId, sensor=$sensorType")
            return null
        }

        val currentValue = (latest["value"] as? Number)?.toDouble() ?: return null
        val currentTime = latest["time"] as? java.sql.Timestamp ?: return null
        val unit = latest["unit"] as? String ?: ""

        // 3. Obtener estadísticas agregadas del período
        val summary =
                statisticsDao.getStatisticsSummary(
                        greenhouseId,
                        sensorType,
                        if (aggregationType == "hourly") hours else days,
                        aggregationType
                )

        val avgValue = (summary["overall_avg"] as? Number)?.toDouble() ?: currentValue
        val minValue = (summary["overall_min"] as? Number)?.toDouble() ?: currentValue
        val maxValue = (summary["overall_max"] as? Number)?.toDouble() ?: currentValue

        // 4. Obtener datos para la gráfica
        val chartData =
                if (aggregationType == "hourly") {
                    val hourlyStats =
                            statisticsDao.getHourlyStatistics(greenhouseId, sensorType, hours)
                    hourlyStats.map { stat ->
                        ChartDataPoint(
                                timestamp = stat.bucket.toString(),
                                value = stat.avgValue ?: 0.0
                        )
                    }
                } else {
                    val dailyStats =
                            statisticsDao.getDailyStatistics(greenhouseId, sensorType, days)
                    dailyStats.map { stat ->
                        ChartDataPoint(
                                timestamp = stat.bucket.toString(),
                                value = stat.avgValue ?: 0.0
                        )
                    }
                }

        // 5. Calcular TREND (↑ +1.2% o ↓ -0.5%)
        val (trendPercent, trendDirection) = calculateTrend(currentValue, avgValue)

        // 6. Construir respuesta
        val startTime = Instant.now().minusSeconds(hours * 3600L + days * 86400L)
        val endTime = currentTime.toInstant()

        return HistoricalDataDto(
                greenhouseId = greenhouseId,
                tenantId = greenhouse.tenantId,
                sensorType = sensorType,
                unit = unit,
                currentValue = currentValue,
                currentValueTimestamp = currentTime.toInstant().toString(),
                avgValue = avgValue,
                minValue = minValue,
                maxValue = maxValue,
                medianValue = null, // Se puede agregar si se necesita
                trendPercent = trendPercent,
                trendDirection = trendDirection,
                chartData = chartData,
                period = period,
                startTime = startTime.toString(),
                endTime = endTime.toString()
        )
    }

    /**
     * Calcula el trend percentage.
     *
     * Ejemplo:
     * - currentValue = 22.5°C
     * - avgValue = 21.8°C
     * - trend = ((22.5 - 21.8) / 21.8) * 100 = +3.2%
     *
     * Pero para la UI, comparamos currentValue con avgValue de las "últimas 24h":
     * - Si current > avg → INCREASING (↑ +X%)
     * - Si current < avg → DECREASING (↓ -X%)
     * - Si diff < 1% → STABLE
     */
    private fun calculateTrend(currentValue: Double, avgValue: Double): Pair<Double, String> {
        if (avgValue == 0.0) {
            return Pair(0.0, "STABLE")
        }

        val percentChange = ((currentValue - avgValue) / avgValue) * 100

        val direction =
                when {
                    abs(percentChange) < 1.0 -> "STABLE"
                    percentChange > 0 -> "INCREASING"
                    else -> "DECREASING"
                }

        return Pair(percentChange, direction)
    }

    /**
     * Parse period string to (hours, days, aggregationType).
     * - "24h" → (24, 0, "hourly")
     * - "7d" → (0, 7, "daily")
     * - "30d" → (0, 30, "daily")
     */
    private fun parsePeriod(period: String): Triple<Int, Int, String> {
        return when {
            period.endsWith("h") -> {
                val hours = period.removeSuffix("h").toIntOrNull() ?: 24
                Triple(hours, 0, "hourly")
            }
            period.endsWith("d") -> {
                val days = period.removeSuffix("d").toIntOrNull() ?: 7
                Triple(0, days, "daily")
            }
            else -> Triple(24, 0, "hourly") // Default: 24h
        }
    }

    /**
     * GET /api/statistics/hourly?greenhouseId=xxx&sensorType=TEMPERATURE&hours=24
     *
     * Obtiene estadísticas horarias.
     */
    fun getHourlyStatistics(
            greenhouseId: Long,
            sensorType: String,
            hours: Int = 24
    ): List<SensorStatisticsHourlyDto> {
        logger.debug(
                "Getting hourly statistics for greenhouse=$greenhouseId, sensor=$sensorType, hours=$hours"
        )
        return statisticsDao.getHourlyStatistics(greenhouseId, sensorType, hours)
    }

    /**
     * GET /api/statistics/daily?greenhouseId=xxx&sensorType=TEMPERATURE&days=7
     *
     * Obtiene estadísticas diarias.
     */
    fun getDailyStatistics(
            greenhouseId: Long,
            sensorType: String,
            days: Int = 7
    ): List<SensorStatisticsDailyDto> {
        logger.debug(
                "Getting daily statistics for greenhouse=$greenhouseId, sensor=$sensorType, days=$days"
        )
        return statisticsDao.getDailyStatistics(greenhouseId, sensorType, days)
    }

    /**
     * GET /api/statistics/summary?greenhouseId=xxx&sensorType=TEMPERATURE&period=24h
     *
     * Obtiene solo el resumen (min/max/avg) sin datos de gráfica.
     */
    fun getStatisticsSummary(
            greenhouseId: Long,
            sensorType: String,
            period: String = "24h"
    ): Map<String, Any?> {
        val (hours, days, aggregationType) = parsePeriod(period)
        return statisticsDao.getStatisticsSummary(
                greenhouseId,
                sensorType,
                if (aggregationType == "hourly") hours else days,
                aggregationType
        )
    }
}

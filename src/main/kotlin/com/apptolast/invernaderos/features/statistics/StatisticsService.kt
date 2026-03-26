package com.apptolast.invernaderos.features.statistics

import com.apptolast.invernaderos.features.device.DeviceRepository
import com.apptolast.invernaderos.features.setting.SettingRepository
import com.apptolast.invernaderos.features.statistics.dao.StatsDao
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.ChartDataPoint
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.HistoricalDataDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsDailyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsHourlyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsMonthlyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsWeeklyDto
import com.apptolast.invernaderos.features.telemetry.timeseries.DeviceCurrentValueRepository
import java.time.Instant
import kotlin.math.abs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * Service para consultas de estadisticas agregadas.
 *
 * Recibe un code como parametro, consulta TimescaleDB para datos temporales,
 * y enriquece con metadatos de negocio desde PostgreSQL.
 *
 * El code es la clave de cruce entre ambas bases de datos.
 */
@Service
class StatisticsService(
    @Qualifier("statsDaoBean") private val statisticsDao: StatsDao,
    private val deviceCurrentValueRepository: DeviceCurrentValueRepository,
    private val deviceRepository: DeviceRepository,
    private val settingRepository: SettingRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Datos completos para la pantalla "Historial de Datos".
     *
     * 1. Obtiene valor actual de device_current_values
     * 2. Obtiene estadisticas de continuous aggregates segun la escala temporal
     * 3. Enriquece con metadatos de negocio desde PostgreSQL (unit)
     *
     * Soporta formatos de periodo:
     * - Horas: "24h", "168h", "720h", "8760h", "87600h"
     * - Dias: "7d", "30d", "365d"
     * - Nombres: "DAY", "WEEK", "MONTH", "YEAR", "ALL", "all"
     */
    fun getHistoricalData(code: String, period: String = "24h"): HistoricalDataDto? {
        logger.debug("Getting historical data for code={}, period={}", code, period)

        val aggregation = parsePeriod(period)

        // 1. Valor actual de device_current_values
        val currentValue = deviceCurrentValueRepository.findByCode(code)
        if (currentValue == null) {
            logger.warn("No current value found for code={}", code)
            return null
        }

        val numericValue = currentValue.value.toDoubleOrNull()
        if (numericValue == null) {
            logger.warn("Current value for code={} is not numeric: {}", code, currentValue.value)
            return null
        }

        // 2. Resolver unidad desde PostgreSQL (device o setting)
        val unit = resolveUnit(code)

        // 3. Estadisticas del periodo
        val summary = statisticsDao.getStatisticsSummary(
            code,
            aggregation.count,
            aggregation.type
        )

        val avgValue = (summary["overall_avg"] as? Number)?.toDouble() ?: numericValue
        val minValue = (summary["overall_min"] as? Number)?.toDouble() ?: numericValue
        val maxValue = (summary["overall_max"] as? Number)?.toDouble() ?: numericValue

        // 4. Datos para grafica (cada escala usa el aggregate optimo)
        val chartData = getChartData(code, aggregation)

        // 5. Trend
        val (trendPercent, trendDirection) = calculateTrend(numericValue, avgValue)

        // 6. Periodo temporal
        val startTime = aggregation.calculateStartTime()

        return HistoricalDataDto(
            code = code,
            unit = unit,
            currentValue = numericValue,
            currentValueTimestamp = currentValue.lastSeenAt.toString(),
            avgValue = avgValue,
            minValue = minValue,
            maxValue = maxValue,
            trendPercent = trendPercent,
            trendDirection = trendDirection,
            chartData = chartData,
            period = period,
            startTime = startTime.toString(),
            endTime = currentValue.lastSeenAt.toString()
        )
    }

    private fun getChartData(code: String, aggregation: AggregationPeriod): List<ChartDataPoint> {
        return when (aggregation.type) {
            "hourly" -> statisticsDao.getHourlyStatistics(code, aggregation.count).map { it.toChartDataPoint() }
            "daily" -> statisticsDao.getDailyStatistics(code, aggregation.count).map { it.toChartDataPoint() }
            "weekly" -> statisticsDao.getWeeklyStatistics(code, aggregation.count).map { it.toChartDataPoint() }
            "monthly" -> statisticsDao.getMonthlyStatistics(code, aggregation.count).map { it.toChartDataPoint() }
            else -> emptyList()
        }
    }

    private fun SensorStatisticsHourlyDto.toChartDataPoint() = ChartDataPoint(
        timestamp = bucket.toString(), value = avgValue ?: 0.0,
        minValue = minValue, maxValue = maxValue
    )

    private fun SensorStatisticsDailyDto.toChartDataPoint() = ChartDataPoint(
        timestamp = bucket.toString(), value = avgValue ?: 0.0,
        minValue = minValue, maxValue = maxValue
    )

    private fun SensorStatisticsWeeklyDto.toChartDataPoint() = ChartDataPoint(
        timestamp = bucket.toString(), value = avgValue ?: 0.0,
        minValue = minValue, maxValue = maxValue
    )

    private fun SensorStatisticsMonthlyDto.toChartDataPoint() = ChartDataPoint(
        timestamp = bucket.toString(), value = avgValue ?: 0.0,
        minValue = minValue, maxValue = maxValue
    )

    /**
     * Estadisticas horarias para un code.
     */
    fun getHourlyStatistics(code: String, hours: Int = 24): List<SensorStatisticsHourlyDto> {
        logger.debug("Getting hourly statistics for code={}, hours={}", code, hours)
        return statisticsDao.getHourlyStatistics(code, hours)
    }

    /**
     * Estadisticas diarias para un code.
     */
    fun getDailyStatistics(code: String, days: Int = 7): List<SensorStatisticsDailyDto> {
        logger.debug("Getting daily statistics for code={}, days={}", code, days)
        return statisticsDao.getDailyStatistics(code, days)
    }

    /**
     * Resumen estadistico (min/max/avg) para un code.
     */
    fun getStatisticsSummary(code: String, period: String = "24h"): Map<String, Any?> {
        val aggregation = parsePeriod(period)
        return statisticsDao.getStatisticsSummary(code, aggregation.count, aggregation.type)
    }

    /**
     * Resuelve la unidad de medida desde PostgreSQL.
     * Busca primero en devices, luego en settings.
     * El EntityGraph carga la relacion unit en la misma query.
     */
    private fun resolveUnit(code: String): String {
        // Intentar buscar como device (DEV-XXXXX)
        val device = deviceRepository.findByCode(code)
        if (device != null) {
            return device.unit?.symbol ?: ""
        }

        // Settings (SET-XXXXX) no tienen relacion unit directa.
        // La unidad se resuelve desde el device asociado si es necesario.
        settingRepository.findByCode(code) ?: return ""

        return ""
    }

    private fun calculateTrend(currentValue: Double, avgValue: Double): Pair<Double, String> {
        if (avgValue == 0.0) return Pair(0.0, "STABLE")

        val percentChange = ((currentValue - avgValue) / avgValue) * 100

        val direction = when {
            abs(percentChange) < 1.0 -> "STABLE"
            percentChange > 0 -> "INCREASING"
            else -> "DECREASING"
        }

        return Pair(percentChange, direction)
    }

    /**
     * Representa un periodo con su estrategia de agregacion.
     *
     * @param count numero de unidades a consultar (horas, dias, semanas o meses)
     * @param type tipo de agregacion: "hourly", "daily", "weekly", "monthly"
     */
    internal data class AggregationPeriod(
        val count: Int,
        val type: String
    ) {
        fun calculateStartTime(): Instant {
            val seconds = when (type) {
                "hourly" -> count * 3600L
                "daily" -> count * 86400L
                "weekly" -> count * 7 * 86400L
                "monthly" -> count * 30 * 86400L
                else -> count * 3600L
            }
            return Instant.now().minusSeconds(seconds)
        }
    }

    /**
     * Parsea el periodo de entrada y determina la estrategia de agregacion optima.
     *
     * Formatos soportados:
     * - Horas: "24h", "168h", "720h", "8760h", "87600h"
     * - Dias: "7d", "30d", "365d"
     * - Nombres: "DAY", "WEEK", "MONTH", "YEAR", "ALL" (case insensitive)
     *
     * Mapeo a continuous aggregates:
     *   <= 48h           -> readings_hourly  (DAY)
     *   <= 7d / 168h     -> readings_daily   (WEEK)
     *   <= 31d / 744h    -> readings_daily   (MONTH)
     *   <= 365d / 8760h  -> readings_weekly  (YEAR)
     *   > 365d           -> readings_monthly (ALL)
     */
    internal fun parsePeriod(period: String): AggregationPeriod {
        // Named periods (case insensitive)
        when (period.uppercase()) {
            "DAY" -> return AggregationPeriod(24, "hourly")
            "WEEK" -> return AggregationPeriod(7, "daily")
            "MONTH" -> return AggregationPeriod(30, "daily")
            "YEAR" -> return AggregationPeriod(52, "weekly")
            "ALL" -> return AggregationPeriod(120, "monthly")
        }

        // Hours format: "24h", "168h", etc.
        if (period.endsWith("h")) {
            val hours = period.removeSuffix("h").toIntOrNull() ?: 24
            return when {
                hours <= 48 -> AggregationPeriod(hours, "hourly")
                hours <= 744 -> AggregationPeriod(hours / 24, "daily")
                hours <= 8760 -> AggregationPeriod(hours / 168, "weekly")
                else -> AggregationPeriod(hours / 720, "monthly")
            }
        }

        // Days format: "7d", "30d", etc.
        if (period.endsWith("d")) {
            val days = period.removeSuffix("d").toIntOrNull() ?: 7
            return when {
                days <= 2 -> AggregationPeriod(days * 24, "hourly")
                days <= 31 -> AggregationPeriod(days, "daily")
                days <= 365 -> AggregationPeriod(days / 7, "weekly")
                else -> AggregationPeriod(days / 30, "monthly")
            }
        }

        // Default: 24 hours
        return AggregationPeriod(24, "hourly")
    }
}

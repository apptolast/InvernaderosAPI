package com.apptolast.invernaderos.features.statistics

import com.apptolast.invernaderos.features.device.DeviceRepository
import com.apptolast.invernaderos.features.setting.SettingRepository
import com.apptolast.invernaderos.features.statistics.dao.StatsDao
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.ChartDataPoint
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.HistoricalDataDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsDailyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsHourlyDto
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
     * 2. Obtiene estadisticas de continuous aggregates
     * 3. Enriquece con metadatos de negocio desde PostgreSQL (unit)
     */
    fun getHistoricalData(code: String, period: String = "24h"): HistoricalDataDto? {
        logger.debug("Getting historical data for code={}, period={}", code, period)

        val (hours, days, aggregationType) = parsePeriod(period)

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
            if (aggregationType == "hourly") hours else days,
            aggregationType
        )

        val avgValue = (summary["overall_avg"] as? Number)?.toDouble() ?: numericValue
        val minValue = (summary["overall_min"] as? Number)?.toDouble() ?: numericValue
        val maxValue = (summary["overall_max"] as? Number)?.toDouble() ?: numericValue

        // 4. Datos para grafica
        val chartData = if (aggregationType == "hourly") {
            statisticsDao.getHourlyStatistics(code, hours).map { stat ->
                ChartDataPoint(timestamp = stat.bucket.toString(), value = stat.avgValue ?: 0.0)
            }
        } else {
            statisticsDao.getDailyStatistics(code, days).map { stat ->
                ChartDataPoint(timestamp = stat.bucket.toString(), value = stat.avgValue ?: 0.0)
            }
        }

        // 5. Trend
        val (trendPercent, trendDirection) = calculateTrend(numericValue, avgValue)

        // 6. Periodo temporal
        val startTime = Instant.now().minusSeconds(hours * 3600L + days * 86400L)

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
        val (hours, days, aggregationType) = parsePeriod(period)
        return statisticsDao.getStatisticsSummary(
            code,
            if (aggregationType == "hourly") hours else days,
            aggregationType
        )
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
            else -> Triple(24, 0, "hourly")
        }
    }
}

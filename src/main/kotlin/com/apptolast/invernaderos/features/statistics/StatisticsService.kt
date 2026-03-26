package com.apptolast.invernaderos.features.statistics

import com.apptolast.invernaderos.features.device.DeviceRepository
import com.apptolast.invernaderos.features.setting.SettingRepository
import com.apptolast.invernaderos.features.statistics.dao.RawReadingRow
import com.apptolast.invernaderos.features.statistics.dao.StatsDao
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.BooleanStatsDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.ChartDataPoint
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.HistoricalDataDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsDailyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsHourlyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsMonthlyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsWeeklyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.TransitionPoint
import com.apptolast.invernaderos.features.telemetry.timeseries.DeviceCurrentValueRepository
import java.time.Instant
import kotlin.math.abs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * Service para consultas de estadisticas agregadas con algoritmo adaptativo.
 *
 * El algoritmo calcula la resolucion optima por sensor segun su frecuencia real:
 * - Sensores de alta frecuencia (>30/h): usa continuous aggregates (hourly/daily/monthly)
 * - Sensores de baja frecuencia (<~4/h): devuelve datos crudos (sin agregar)
 * - Sensores booleanos (REGANDO, EN COLA): devuelve transiciones + % tiempo encendido
 *
 * El code es la clave de cruce entre TimescaleDB (datos temporales) y PostgreSQL (metadatos).
 */
@Service
class StatisticsService(
    @Qualifier("statsDaoBean") private val statisticsDao: StatsDao,
    private val deviceCurrentValueRepository: DeviceCurrentValueRepository,
    private val deviceRepository: DeviceRepository,
    private val settingRepository: SettingRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /** Maximo de puntos que el movil puede renderizar eficientemente */
        const val MAX_CHART_POINTS = 720L
    }

    /**
     * Resolucion optima calculada por el algoritmo adaptativo.
     */
    enum class Resolution { RAW, HOURLY, DAILY, MONTHLY }

    /**
     * Datos completos para la pantalla "Historial de Datos".
     *
     * Algoritmo adaptativo:
     * 1. Detecta si el sensor es booleano → devuelve transiciones
     * 2. Calcula la densidad del sensor (lecturas/hora)
     * 3. Elige la resolucion optima segun densidad × periodo
     *
     * Soporta formatos de periodo:
     * - Nombres: "DAY", "WEEK", "MONTH", "YEAR", "ALL" (case insensitive)
     * - Horas: "24h", "168h", "720h", "8760h", "87600h"
     * - Dias: "7d", "30d", "365d"
     */
    fun getHistoricalData(code: String, period: String = "24h"): HistoricalDataDto? {
        logger.debug("Getting historical data for code={}, period={}", code, period)

        val hoursInPeriod = calculateHoursInPeriod(period)

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

        // 2. Detectar booleano
        if (isBooleanDevice(code)) {
            return getBooleanHistoricalData(code, hoursInPeriod, period, numericValue, currentValue.lastSeenAt)
        }

        // 3. Calcular resolucion adaptativa
        val density = statisticsDao.getReadingDensity(code)
        val resolution = calculateResolution(density, hoursInPeriod)
        logger.debug("Adaptive resolution for code={}: density={}/h, period={}h, resolution={}",
            code, String.format("%.1f", density), hoursInPeriod, resolution)

        // 4. Resolver unidad desde PostgreSQL
        val unit = resolveUnit(code)

        // 5. Obtener datos segun resolucion
        val chartData = getAdaptiveChartData(code, hoursInPeriod, resolution)

        // 6. Estadisticas del periodo
        val summaryResolution = when (resolution) {
            Resolution.RAW -> if (hoursInPeriod <= 48) "hourly" else "daily"
            Resolution.HOURLY -> "hourly"
            Resolution.DAILY -> "daily"
            Resolution.MONTHLY -> "monthly"
        }
        val summaryCount = when (summaryResolution) {
            "hourly" -> hoursInPeriod.toInt()
            "daily" -> (hoursInPeriod / 24).coerceAtLeast(1).toInt()
            "monthly" -> (hoursInPeriod / 720).coerceAtLeast(1).toInt()
            else -> hoursInPeriod.toInt()
        }
        val summary = statisticsDao.getStatisticsSummary(code, summaryCount, summaryResolution)

        val avgValue = (summary["overall_avg"] as? Number)?.toDouble() ?: numericValue
        val minValue = (summary["overall_min"] as? Number)?.toDouble() ?: numericValue
        val maxValue = (summary["overall_max"] as? Number)?.toDouble() ?: numericValue

        // 7. Trend
        val (trendPercent, trendDirection) = calculateTrend(numericValue, avgValue)

        // 8. Periodo temporal
        val startTime = Instant.now().minusSeconds(hoursInPeriod * 3600)

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
            endTime = currentValue.lastSeenAt.toString(),
            resolution = resolution.name.lowercase(),
            pointCount = chartData.size
        )
    }

    /**
     * Algoritmo adaptativo: elige la resolucion optima segun la densidad del sensor
     * y el periodo solicitado.
     *
     * Logica:
     * - Si el total estimado de lecturas cabe en el limite → datos crudos (RAW)
     * - Si las horas del periodo caben en el limite → hourly (1 punto/hora)
     * - Si los dias caben en el limite → daily (1 punto/dia)
     * - Sino → monthly
     */
    internal fun calculateResolution(density: Double, hoursInPeriod: Long): Resolution {
        val totalEstimated = (density * hoursInPeriod).toLong()

        return when {
            totalEstimated <= MAX_CHART_POINTS -> Resolution.RAW
            hoursInPeriod <= MAX_CHART_POINTS -> Resolution.HOURLY
            hoursInPeriod / 24 <= MAX_CHART_POINTS -> Resolution.DAILY
            else -> Resolution.MONTHLY
        }
    }

    private fun getAdaptiveChartData(code: String, hoursInPeriod: Long, resolution: Resolution): List<ChartDataPoint> {
        return when (resolution) {
            Resolution.RAW -> {
                statisticsDao.getRawReadings(code, hoursInPeriod).map { it.toChartDataPoint() }
            }
            Resolution.HOURLY -> {
                statisticsDao.getHourlyStatistics(code, hoursInPeriod.toInt()).map { it.toChartDataPoint() }
            }
            Resolution.DAILY -> {
                val days = (hoursInPeriod / 24).coerceAtLeast(1).toInt()
                statisticsDao.getDailyStatistics(code, days).map { it.toChartDataPoint() }
            }
            Resolution.MONTHLY -> {
                val months = (hoursInPeriod / 720).coerceAtLeast(1).toInt()
                statisticsDao.getMonthlyStatistics(code, months).map { it.toChartDataPoint() }
            }
        }
    }

    /**
     * Datos historicos para un dispositivo booleano (REGANDO, EN COLA, etc.)
     * Devuelve transiciones de estado + estadisticas de uso.
     */
    private fun getBooleanHistoricalData(
        code: String,
        hoursInPeriod: Long,
        period: String,
        currentNumericValue: Double,
        lastSeenAt: Instant
    ): HistoricalDataDto {
        val readings = statisticsDao.getRawReadings(code, hoursInPeriod)
        val unit = resolveUnit(code)

        // Calcular transiciones (solo donde el valor cambia)
        val transitions = mutableListOf<TransitionPoint>()
        if (readings.isNotEmpty()) {
            // Primera lectura siempre es una "transicion" (estado inicial)
            transitions.add(TransitionPoint(
                timestamp = readings.first().time.toString(),
                newState = (readings.first().numericValue ?: 0.0) == 1.0
            ))
            // Detectar cambios subsiguientes
            readings.zipWithNext().forEach { (prev, curr) ->
                if (prev.numericValue != curr.numericValue) {
                    transitions.add(TransitionPoint(
                        timestamp = curr.time.toString(),
                        newState = (curr.numericValue ?: 0.0) == 1.0
                    ))
                }
            }
        }

        // Estadisticas booleanas
        val onCount = readings.count { (it.numericValue ?: 0.0) == 1.0 }
        val total = readings.size
        val onPct = if (total > 0) (onCount.toDouble() / total * 100) else 0.0

        val startTime = Instant.now().minusSeconds(hoursInPeriod * 3600)

        return HistoricalDataDto(
            code = code,
            unit = unit,
            currentValue = currentNumericValue,
            currentValueTimestamp = lastSeenAt.toString(),
            avgValue = onPct / 100.0,
            minValue = 0.0,
            maxValue = 1.0,
            trendPercent = 0.0,
            trendDirection = "STABLE",
            chartData = emptyList(),
            period = period,
            startTime = startTime.toString(),
            endTime = lastSeenAt.toString(),
            resolution = "transitions",
            pointCount = transitions.size,
            isBooleanDevice = true,
            transitions = transitions,
            booleanStats = BooleanStatsDto(
                transitionCount = (transitions.size - 1).coerceAtLeast(0),
                onPercentage = onPct,
                offPercentage = 100.0 - onPct
            )
        )
    }

    /**
     * Detecta si un code corresponde a un dispositivo booleano.
     * Los settings con dataType BOOLEAN son booleanos.
     * Los devices tipo REGANDO/EN COLA con solo 2 valores distintos son booleanos.
     */
    private fun isBooleanDevice(code: String): Boolean {
        if (code.startsWith("SET-")) {
            val setting = settingRepository.findByCode(code)
            return setting?.dataType?.name?.uppercase() == "BOOLEAN"
        }
        if (code.startsWith("DEV-")) {
            val device = deviceRepository.findByCode(code)
            if (device != null) {
                val typeName = device.type?.name?.uppercase() ?: ""
                return typeName in listOf("REGANDO", "EN COLA")
            }
        }
        return false
    }

    // ── Extension functions for chart data point conversion ──────────────────

    private fun RawReadingRow.toChartDataPoint() = ChartDataPoint(
        timestamp = time.toString(), value = numericValue ?: 0.0
    )

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

    // ── Public methods for direct aggregate access (used by StatisticsController) ──

    fun getHourlyStatistics(code: String, hours: Int = 24): List<SensorStatisticsHourlyDto> {
        logger.debug("Getting hourly statistics for code={}, hours={}", code, hours)
        return statisticsDao.getHourlyStatistics(code, hours)
    }

    fun getDailyStatistics(code: String, days: Int = 7): List<SensorStatisticsDailyDto> {
        logger.debug("Getting daily statistics for code={}, days={}", code, days)
        return statisticsDao.getDailyStatistics(code, days)
    }

    fun getStatisticsSummary(code: String, period: String = "24h"): Map<String, Any?> {
        val hoursInPeriod = calculateHoursInPeriod(period)
        val (summaryType, summaryCount) = when {
            hoursInPeriod <= 48 -> "hourly" to hoursInPeriod.toInt()
            hoursInPeriod <= 720 -> "daily" to (hoursInPeriod / 24).toInt()
            else -> "monthly" to (hoursInPeriod / 720).toInt()
        }
        return statisticsDao.getStatisticsSummary(code, summaryCount, summaryType)
    }

    // ── Utility methods ─────────────────────────────────────────────────────

    private fun resolveUnit(code: String): String {
        val device = deviceRepository.findByCode(code)
        if (device != null) {
            return device.unit?.symbol ?: ""
        }
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
     * Calcula las horas de un periodo. Soporta nombres (DAY, WEEK, MONTH, YEAR, ALL),
     * formato horas (24h, 168h) y formato dias (7d, 30d).
     */
    internal fun calculateHoursInPeriod(period: String): Long {
        // Named periods (case insensitive)
        when (period.uppercase()) {
            "DAY" -> return 24L
            "WEEK" -> return 168L
            "MONTH" -> return 720L
            "YEAR" -> return 8760L
            "ALL" -> return 87600L
        }
        // Hours: "24h", "168h"
        if (period.endsWith("h")) {
            return period.removeSuffix("h").toLongOrNull() ?: 24L
        }
        // Days: "7d", "30d"
        if (period.endsWith("d")) {
            val days = period.removeSuffix("d").toLongOrNull() ?: 7L
            return days * 24
        }
        return 24L
    }
}

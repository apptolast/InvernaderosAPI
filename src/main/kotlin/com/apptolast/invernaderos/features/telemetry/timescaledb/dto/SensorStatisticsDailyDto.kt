package com.apptolast.invernaderos.features.telemetry.timescaledb.dto

import java.time.Instant

/**
 * DTO para estadísticas diarias de sensores.
 * Mapea resultados de: iot.cagg_sensor_readings_daily (continuous aggregate)
 *
 * Usado en pantalla "Historial de Datos" para:
 * - Gráficas de últimos 7 días con buckets de 1 día
 * - Gráficas de último mes con buckets de 1 día
 * - Calcular min/max/promedio/mediana
 * - Detectar tendencias (trend indicator)
 */
data class SensorStatisticsDailyDto(
    /**
     * Start of the 1-day bucket (e.g., 2025-01-15 00:00:00)
     */
    val bucket: Instant,

    val greenhouseId: Long,
    val tenantId: Long,
    val sensorType: String,
    val unit: String?,

    // Statistical aggregates
    val avgValue: Double?,
    val minValue: Double?,
    val maxValue: Double?,
    val stddevValue: Double?,
    val countReadings: Long,

    // Extended statistics
    val medianValue: Double?,
    val p95Value: Double?,  // 95th percentile
    val p5Value: Double?,   // 5th percentile

    // Quality metrics
    val nullCount: Long?,
    val hoursWithData: Short?,  // 0-24 hours that had readings

    // Time tracking
    val firstReadingAt: Instant?,
    val lastReadingAt: Instant?
) {
    /**
     * Calcula data completeness percentage
     * Asumiendo 1 reading/min = 1440 readings/día
     */
    fun calculateCompleteness(expectedPerDay: Int = 1440): Double {
        return (countReadings.toDouble() / expectedPerDay * 100).coerceAtMost(100.0)
    }

    /**
     * Determina si este día tiene datos completos
     */
    fun hasCompleteData(): Boolean {
        return (hoursWithData ?: 0) >= 20  // Al menos 20 de 24 horas
    }

    /**
     * Calcula el rango de valores (max - min)
     */
    fun getValueRange(): Double? {
        return if (maxValue != null && minValue != null) {
            maxValue - minValue
        } else null
    }
}

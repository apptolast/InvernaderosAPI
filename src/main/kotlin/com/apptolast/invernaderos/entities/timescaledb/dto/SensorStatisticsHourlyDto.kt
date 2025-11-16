package com.apptolast.invernaderos.entities.timescaledb.dto

import java.time.Instant
import java.util.UUID

/**
 * DTO para estadísticas horarias de sensores.
 * Mapea resultados de: iot.cagg_sensor_readings_hourly (continuous aggregate)
 *
 * Usado en pantalla "Historial de Datos" para:
 * - Gráficas de últimas 24h con buckets de 1 hora
 * - Calcular min/max/promedio para mostrar en cards
 */
data class SensorStatisticsHourlyDto(
    /**
     * Start of the 1-hour bucket (e.g., 2025-01-15 14:00:00)
     */
    val bucket: Instant,

    val greenhouseId: UUID,
    val tenantId: UUID,
    val sensorType: String,  // TEMPERATURE, HUMIDITY, CO2, etc.
    val unit: String?,       // °C, %, ppm, etc.

    // Statistical aggregates
    val avgValue: Double?,
    val minValue: Double?,
    val maxValue: Double?,
    val stddevValue: Double?,
    val countReadings: Long,

    // Quality metrics
    val nullCount: Long?,

    // Time tracking
    val firstReadingAt: Instant?,
    val lastReadingAt: Instant?
) {
    /**
     * Calcula data completeness percentage (asumiendo 1 reading/min = 60/hora)
     */
    fun calculateCompleteness(expectedPerHour: Int = 60): Double {
        return (countReadings.toDouble() / expectedPerHour * 100).coerceAtMost(100.0)
    }

    /**
     * Determina si esta hora tiene datos de calidad
     */
    fun hasGoodQuality(): Boolean {
        return countReadings > 0 && (nullCount ?: 0) < (countReadings * 0.1)
    }
}

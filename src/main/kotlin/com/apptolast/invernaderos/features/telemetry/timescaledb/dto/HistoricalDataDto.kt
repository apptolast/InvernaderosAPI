package com.apptolast.invernaderos.features.telemetry.timescaledb.dto

import java.util.UUID

/**
 * DTO para la pantalla "Historial de Datos" del frontend Kotlin Multiplatform.
 *
 * Corresponde exactamente a lo que se muestra en la UI:
 * - Temperatura actual: 22.5°C
 * - Promedio: 21.8°C
 * - Máx: 24.1°C
 * - Mín: 19.5°C
 * - Trend: +1.2% (últimas 24h ↑)
 * - Gráfica con datos del período seleccionado
 */
data class HistoricalDataDto(
    val greenhouseId: UUID,
    val tenantId: UUID,
    val sensorType: String,  // TEMPERATURE, HUMIDITY, CO2, etc.
    val unit: String,         // °C, %, ppm, etc.

    // Valor actual (última lectura)
    val currentValue: Double,
    val currentValueTimestamp: String,  // ISO 8601

    // Estadísticas del período
    val avgValue: Double,
    val minValue: Double,
    val maxValue: Double,
    val medianValue: Double?,

    // Trend indicator (para mostrar "↑ +1.2%" o "↓ -0.5%")
    val trendPercent: Double,      // +1.2, -0.5, etc. (positive = increasing, negative = decreasing)
    val trendDirection: String,    // "INCREASING", "DECREASING", "STABLE"

    // Datos para la gráfica (time-series points)
    val chartData: List<ChartDataPoint>,

    // Período de los datos
    val period: String,  // "24h", "7d", "30d"
    val startTime: String,  // ISO 8601
    val endTime: String     // ISO 8601
)

/**
 * Punto de datos para la gráfica de tiempo.
 */
data class ChartDataPoint(
    val timestamp: String,  // ISO 8601
    val value: Double
)

/**
 * DTO para resumen de múltiples sensores.
 * Usado en dashboard principal.
 */
data class GreenhouseConditionsSummaryDto(
    val greenhouseId: UUID,
    val tenantId: UUID,
    val timestamp: String,  // ISO 8601

    // Temperature
    val temperature: SensorSummary?,

    // Humidity
    val humidity: SensorSummary?,

    // Light
    val light: SensorSummary?,

    // CO2
    val co2: SensorSummary?
)

/**
 * Resumen de un sensor específico.
 */
data class SensorSummary(
    val current: Double,
    val avg: Double,
    val min: Double,
    val max: Double,
    val unit: String,
    val trendPercent: Double
)

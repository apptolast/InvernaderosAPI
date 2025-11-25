package com.apptolast.invernaderos.features.statistics

import java.time.Instant

/**
 * DTO para estadísticas de sensores y setpoints del invernadero
 *
 * Contiene métricas agregadas calculadas sobre un periodo de tiempo
 */
data class GreenhouseStatisticsDto(
    val sensorId: String,
    val sensorType: String, // "SENSOR" o "SETPOINT"
    val min: Double?,
    val max: Double?,
    val avg: Double?,
    val count: Long,
    val lastValue: Double?,
    val lastTimestamp: Instant?,
    val periodStart: Instant,
    val periodEnd: Instant
)

/**
 * DTO para resumen de estadísticas de todos los sensores y setpoints
 */
data class GreenhouseSummaryDto(
    val timestamp: Instant,
    val totalMessages: Long,
    val sensors: Map<String, SensorSummary>,
    val setpoints: Map<String, SensorSummary>,
    val periodStart: Instant?,
    val periodEnd: Instant?
)

/**
 * Resumen individual de un sensor o setpoint
 */
data class SensorSummary(
    val current: Double?,
    val min: Double?,
    val max: Double?,
    val avg: Double?,
    val count: Long
)

/**
 * DTO para solicitud de estadísticas con filtros
 */
data class StatisticsRequestDto(
    val sensorId: String? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val period: String? = null // "1h", "24h", "7d", "30d"
)

package com.apptolast.invernaderos.features.statistics

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * DTO para estadísticas de sensores y setpoints del invernadero
 *
 * Contiene métricas agregadas calculadas sobre un periodo de tiempo
 */
@Schema(description = "DTO for greenhouse sensor statistics")
data class GreenhouseStatisticsDto(
        @Schema(description = "Sensor identifier") val sensorId: String,
        @Schema(description = "Sensor type (SENSOR or SETPOINT)")
        val sensorType: String, // "SENSOR" o "SETPOINT"
        @Schema(description = "Minimum value") val min: Double?,
        @Schema(description = "Maximum value") val max: Double?,
        @Schema(description = "Average value") val avg: Double?,
        @Schema(description = "Count of readings") val count: Long,
        @Schema(description = "Last recorded value") val lastValue: Double?,
        @Schema(description = "Timestamp of last reading") val lastTimestamp: Instant?,
        @Schema(description = "Start of the period") val periodStart: Instant,
        @Schema(description = "End of the period") val periodEnd: Instant
)

/** DTO para resumen de estadísticas de todos los sensores y setpoints */
@Schema(description = "Summary of statistics for all sensors")
data class GreenhouseSummaryDto(
        @Schema(description = "Current timestamp") val timestamp: Instant,
        @Schema(description = "Total messages processed") val totalMessages: Long,
        @Schema(description = "Map of sensor summaries") val sensors: Map<String, SensorSummary>,
        @Schema(description = "Map of setpoint summaries")
        val setpoints: Map<String, SensorSummary>,
        @Schema(description = "Start of the period") val periodStart: Instant?,
        @Schema(description = "End of the period") val periodEnd: Instant?
)

/** Resumen individual de un sensor o setpoint */
@Schema(description = "Summary for a single sensor")
data class SensorSummary(
        @Schema(description = "Current value") val current: Double?,
        @Schema(description = "Minimum value") val min: Double?,
        @Schema(description = "Maximum value") val max: Double?,
        @Schema(description = "Average value") val avg: Double?,
        @Schema(description = "Count of readings") val count: Long
)

/** DTO para solicitud de estadísticas con filtros */
@Schema(description = "Request for statistics with filters")
data class StatisticsRequestDto(
        @Schema(description = "Sensor identifier") val sensorId: String? = null,
        @Schema(description = "Start time filter") val startTime: Instant? = null,
        @Schema(description = "End time filter") val endTime: Instant? = null,
        @Schema(description = "Period (e.g., 1h, 24h)")
        val period: String? = null // "1h", "24h", "7d", "30d"
)

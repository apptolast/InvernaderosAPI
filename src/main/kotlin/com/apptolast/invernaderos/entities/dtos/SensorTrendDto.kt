package com.apptolast.invernaderos.entities.dtos

import java.time.Instant

/**
 * DTO for sensor trend calculation
 * Shows percentage change and direction over a time period
 *
 * Used by: GET /api/sensors/stats/{sensorId}/trend
 * Frontend: "Historial de Datos" screen - trend indicator (+1.2%)
 */
data class SensorTrendDto(
    /**
     * Sensor identifier (e.g., "TEMPERATURA INVERNADERO 01")
     */
    val sensorId: String,

    /**
     * Current (most recent) sensor value
     */
    val currentValue: Double,

    /**
     * Previous (oldest) sensor value in the period
     */
    val previousValue: Double,

    /**
     * Percentage change: ((current - previous) / previous) * 100
     * Example: 1.2 means +1.2% increase
     */
    val percentageChange: Double,

    /**
     * Absolute change: current - previous
     * Example: 0.27 means 0.27°C increase
     */
    val absoluteChange: Double,

    /**
     * Trend direction: "UP", "DOWN", or "STABLE"
     * - UP: percentageChange > 0.5
     * - DOWN: percentageChange < -0.5
     * - STABLE: -0.5 <= percentageChange <= 0.5
     */
    val direction: String,

    /**
     * Time period for trend calculation
     * Values: "1h", "24h", "7d", "30d"
     */
    val period: String,

    /**
     * Timestamp of current (most recent) value
     */
    val currentTimestamp: Instant,

    /**
     * Timestamp of previous (oldest) value in period
     */
    val previousTimestamp: Instant,

    /**
     * Sensor unit (e.g., "°C", "%", "ppm")
     */
    val unit: String? = null
) {
    companion object {
        /**
         * Determine trend direction based on percentage change
         * Uses 0.5% threshold to avoid noise
         */
        fun calculateDirection(percentageChange: Double): String = when {
            percentageChange > 0.5 -> "UP"
            percentageChange < -0.5 -> "DOWN"
            else -> "STABLE"
        }
    }
}

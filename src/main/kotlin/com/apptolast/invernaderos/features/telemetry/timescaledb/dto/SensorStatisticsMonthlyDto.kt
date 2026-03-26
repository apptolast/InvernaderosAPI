package com.apptolast.invernaderos.features.telemetry.timescaledb.dto

import java.time.Instant

/**
 * DTO para estadisticas mensuales de un code.
 * Mapea resultados de: iot.readings_monthly (continuous aggregate)
 *
 * Solo contiene datos temporales de TimescaleDB.
 * Los metadatos de negocio (sensor_type, unit, greenhouse) se resuelven
 * desde PostgreSQL en la capa de servicio.
 */
data class SensorStatisticsMonthlyDto(
    val bucket: Instant,
    val code: String,
    val avgValue: Double?,
    val minValue: Double?,
    val maxValue: Double?,
    val stddevValue: Double?,
    val countReadings: Long
)

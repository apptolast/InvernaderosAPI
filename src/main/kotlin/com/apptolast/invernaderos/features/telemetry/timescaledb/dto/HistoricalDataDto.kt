package com.apptolast.invernaderos.features.telemetry.timescaledb.dto

/**
 * DTO para la pantalla "Historial de Datos" del frontend Kotlin Multiplatform.
 *
 * Se construye cruzando datos de:
 * - TimescaleDB: valores temporales via code (current value, avg, min, max, chart data)
 * - PostgreSQL: metadatos de negocio via code (unit, sensor_type, greenhouse)
 *
 * El code es la clave de cruce entre ambas bases de datos.
 */
data class HistoricalDataDto(
    val code: String,

    // Metadatos de negocio (resueltos desde PostgreSQL)
    val unit: String,

    // Valor actual (ultima lectura de device_current_values)
    val currentValue: Double,
    val currentValueTimestamp: String,

    // Estadisticas del periodo (de continuous aggregates)
    val avgValue: Double,
    val minValue: Double,
    val maxValue: Double,

    // Trend indicator
    val trendPercent: Double,
    val trendDirection: String,

    // Datos para la grafica (time-series points)
    val chartData: List<ChartDataPoint>,

    // Periodo de los datos
    val period: String,
    val startTime: String,
    val endTime: String,

    // Metadata de resolucion adaptativa (algoritmo decide segun frecuencia del sensor)
    val resolution: String = "hourly",
    val pointCount: Int = 0,

    // Datos booleanos opcionales (REGANDO, EN COLA, etc.)
    val isBooleanDevice: Boolean = false,
    val transitions: List<TransitionPoint>? = null,
    val booleanStats: BooleanStatsDto? = null
)

/**
 * Transicion de estado de un dispositivo booleano.
 */
data class TransitionPoint(
    val timestamp: String,
    val newState: Boolean
)

/**
 * Estadisticas de un dispositivo booleano en un periodo.
 */
data class BooleanStatsDto(
    val transitionCount: Int,
    val onPercentage: Double,
    val offPercentage: Double
)

/**
 * Punto de datos para la grafica de tiempo.
 *
 * value = avg_value del bucket (backward compatible).
 * minValue/maxValue permiten visualizar rangos en la grafica.
 */
data class ChartDataPoint(
    val timestamp: String,
    val value: Double,
    val minValue: Double? = null,
    val maxValue: Double? = null
)

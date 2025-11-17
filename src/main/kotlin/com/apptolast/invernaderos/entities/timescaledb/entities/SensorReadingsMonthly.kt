package com.apptolast.invernaderos.entities.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Composite ID for SensorReadingsMonthly entity.
 * Necesaria para la clave primaria compuesta (time, greenhouseId, sensorType)
 */
data class SensorReadingsMonthlyId(
    val time: Instant,
    val greenhouseId: UUID,
    val sensorType: String
) : Serializable {
    // JPA requires a public no-arg constructor
    constructor() : this(
        Instant.EPOCH,
        UUID(0, 0),
        ""
    )
}

/**
 * Entity para almacenar agregaciones mensuales de lecturas de sensores.
 * Pre-computed monthly statistics optimizadas para comparaciones año-a-año y tendencias a largo plazo.
 *
 * Tabla: iot.sensor_readings_monthly (TimescaleDB hypertable)
 * Uso: Comparaciones YoY, tendencias a largo plazo, reportes anuales
 * Tamaño esperado: ~12 rows/año por greenhouse/sensor_type
 *
 * @property time Inicio del bucket de 1 mes (ej: 2025-01-01 00:00:00 representa todo enero)
 * @property greenhouseId UUID del invernadero
 * @property tenantId UUID del tenant (denormalizado para queries multi-tenant)
 * @property sensorType Tipo de sensor (TEMPERATURE, HUMIDITY, etc.)
 * @property avgValue Valor promedio del mes
 * @property minValue Valor mínimo del mes
 * @property maxValue Valor máximo del mes
 * @property stddevValue Desviación estándar del mes
 * @property countReadings Número de lecturas agregadas en este mes
 * @property medianValue Mediana (percentil 50)
 * @property p95Value Percentil 95
 * @property p5Value Percentil 5
 * @property nullCount Número de lecturas NULL
 * @property outOfRangeCount Lecturas fuera de umbrales
 * @property dataCompletenessPercent Porcentaje de completitud de datos
 * @property trend Clasificación de tendencia: INCREASING, DECREASING, STABLE, VOLATILE
 * @property monthOverMonthChangePercent Cambio porcentual respecto al mes anterior
 * @property unit Unidad de medida
 * @property firstReadingAt Timestamp de la primera lectura del mes
 * @property lastReadingAt Timestamp de la última lectura del mes
 * @property daysWithData Número de días que tuvieron lecturas
 * @property createdAt Fecha de creación del registro
 * @property updatedAt Fecha de última actualización
 */
@Entity
@Table(
    name = "sensor_readings_monthly",
    schema = "iot",
    indexes = [
        Index(name = "idx_monthly_greenhouse_time", columnList = "greenhouse_id, time"),
        Index(name = "idx_monthly_tenant_time", columnList = "tenant_id, time"),
        Index(name = "idx_monthly_greenhouse_sensor_type", columnList = "greenhouse_id, sensor_type, time"),
        Index(name = "idx_monthly_tenant_sensor_type", columnList = "tenant_id, sensor_type, time"),
        Index(name = "idx_monthly_composite", columnList = "tenant_id, greenhouse_id, sensor_type, time")
    ]
)
@IdClass(SensorReadingsMonthlyId::class)
data class SensorReadingsMonthly(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Id
    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Id
    @Column(name = "sensor_type", nullable = false, length = 30)
    val sensorType: String,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    // Aggregated statistics
    @Column(name = "avg_value", columnDefinition = "double precision")
    val avgValue: Double? = null,

    @Column(name = "min_value", columnDefinition = "double precision")
    val minValue: Double? = null,

    @Column(name = "max_value", columnDefinition = "double precision")
    val maxValue: Double? = null,

    @Column(name = "stddev_value", columnDefinition = "double precision")
    val stddevValue: Double? = null,

    @Column(name = "count_readings", nullable = false)
    val countReadings: Long,

    // Extended monthly statistics
    @Column(name = "median_value", columnDefinition = "double precision")
    val medianValue: Double? = null,

    @Column(name = "p95_value", columnDefinition = "double precision")
    val p95Value: Double? = null,

    @Column(name = "p5_value", columnDefinition = "double precision")
    val p5Value: Double? = null,

    // Quality metrics
    @Column(name = "null_count")
    val nullCount: Long? = 0,

    @Column(name = "out_of_range_count")
    val outOfRangeCount: Long? = 0,

    @Column(name = "data_completeness_percent", precision = 5, scale = 2)
    val dataCompletenessPercent: BigDecimal? = null,

    // Trend analysis
    /**
     * Clasificación de tendencia basada en stddev y cambio MoM.
     * Valores: INCREASING, DECREASING, STABLE, VOLATILE
     */
    @Column(length = 20)
    val trend: String? = null,

    /**
     * Cambio porcentual comparado con el promedio del mes anterior.
     */
    @Column(name = "month_over_month_change_percent", precision = 10, scale = 2)
    val monthOverMonthChangePercent: BigDecimal? = null,

    // Time-based metrics
    @Column(length = 20)
    val unit: String? = null,

    @Column(name = "first_reading_at")
    val firstReadingAt: Instant? = null,

    @Column(name = "last_reading_at")
    val lastReadingAt: Instant? = null,

    @Column(name = "days_with_data", columnDefinition = "SMALLINT")
    val daysWithData: Short? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    companion object {
        object Trend {
            const val INCREASING = "INCREASING"
            const val DECREASING = "DECREASING"
            const val STABLE = "STABLE"
            const val VOLATILE = "VOLATILE"
        }
    }
}

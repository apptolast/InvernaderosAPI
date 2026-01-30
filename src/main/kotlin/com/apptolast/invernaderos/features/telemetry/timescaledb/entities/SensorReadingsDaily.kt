package com.apptolast.invernaderos.features.telemetry.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Composite ID for SensorReadingsDaily entity.
 * Necesaria para la clave primaria compuesta (time, greenhouseId, sensorType)
 */
data class SensorReadingsDailyId(
    val time: Instant,
    val greenhouseId: Long,
    val sensorType: String
) : Serializable {
    // Explicit no-arg constructor for JPA
    constructor() : this(
        Instant.EPOCH,
        0L,
        ""
    )
}

/**
 * Entity para almacenar agregaciones diarias de lecturas de sensores.
 * Pre-computed daily statistics optimizadas para reportes semanales/mensuales y análisis de tendencias.
 *
 * Tabla: iot.sensor_readings_daily (TimescaleDB hypertable)
 * Uso: Reportes semanales/mensuales, análisis de tendencias, endpoint /statistics/daily
 * Tamaño esperado: ~365 rows/año por greenhouse/sensor_type
 *
 * @property time Inicio del bucket de 1 día (ej: 2025-01-15 00:00:00 representa todo el día)
 * @property greenhouseId UUID del invernadero
 * @property tenantId UUID del tenant (denormalizado para queries multi-tenant)
 * @property sensorType Tipo de sensor (TEMPERATURE, HUMIDITY, etc.)
 * @property avgValue Valor promedio del día
 * @property minValue Valor mínimo del día
 * @property maxValue Valor máximo del día
 * @property stddevValue Desviación estándar del día
 * @property countReadings Número de lecturas agregadas en este día
 * @property medianValue Mediana (percentil 50)
 * @property p95Value Percentil 95 (detección de picos)
 * @property p5Value Percentil 5 (detección de mínimos)
 * @property nullCount Número de lecturas NULL
 * @property outOfRangeCount Lecturas fuera de umbrales
 * @property dataCompletenessPercent Porcentaje de completitud de datos (count_readings / expected_readings * 100)
 * @property unit Unidad de medida
 * @property firstReadingAt Timestamp de la primera lectura del día
 * @property lastReadingAt Timestamp de la última lectura del día
 * @property hoursWithData Número de horas (0-24) que tuvieron lecturas
 * @property createdAt Fecha de creación del registro
 * @property updatedAt Fecha de última actualización
 */
@Entity
@Table(
    name = "sensor_readings_daily",
    schema = "iot",
    indexes = [
        Index(name = "idx_daily_greenhouse_time", columnList = "greenhouse_id, time"),
        Index(name = "idx_daily_tenant_time", columnList = "tenant_id, time"),
        Index(name = "idx_daily_greenhouse_sensor_type", columnList = "greenhouse_id, sensor_type, time"),
        Index(name = "idx_daily_tenant_sensor_type", columnList = "tenant_id, sensor_type, time"),
        Index(name = "idx_daily_composite", columnList = "tenant_id, greenhouse_id, sensor_type, time")
    ]
)
@IdClass(SensorReadingsDailyId::class)
data class SensorReadingsDaily(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Id
    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: Long,

    @Id
    @Column(name = "sensor_type", nullable = false, length = 30)
    val sensorType: String,

    @Column(name = "tenant_id")
    val tenantId: Long?,

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

    // Extended daily statistics
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

    // Time-based metrics
    @Column(length = 20)
    val unit: String? = null,

    @Column(name = "first_reading_at")
    val firstReadingAt: Instant? = null,

    @Column(name = "last_reading_at")
    val lastReadingAt: Instant? = null,

    @Column(name = "hours_with_data", columnDefinition = "SMALLINT")
    val hoursWithData: Short? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

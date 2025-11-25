package com.apptolast.invernaderos.features.telemetry.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Composite ID for SensorReadingsHourly entity.
 * Necesaria para la clave primaria compuesta (time, greenhouseId, sensorType)
 */
data class SensorReadingsHourlyId(
    val time: Instant,
    val greenhouseId: UUID,
    val sensorType: String
) : Serializable {
    // Explicit no-arg constructor for JPA
    constructor() : this(
        Instant.EPOCH,
        UUID(0, 0),
        ""
    )
}

/**
 * Entity para almacenar agregaciones horarias de lecturas de sensores.
 * Pre-computed hourly statistics optimizadas para consultas de dashboard y reportes.
 *
 * Tabla: iot.sensor_readings_hourly (TimescaleDB hypertable)
 * Uso: Dashboard charts, reportes horarios, endpoint /statistics/hourly
 * Tamaño esperado: ~8,760 rows/año por greenhouse/sensor_type (24h * 365 días)
 *
 * @property time Inicio del bucket de 1 hora (ej: 2025-01-15 14:00:00 representa 14:00-15:00)
 * @property greenhouseId UUID del invernadero
 * @property tenantId UUID del tenant (denormalizado para queries multi-tenant)
 * @property sensorType Tipo de sensor (TEMPERATURE, HUMIDITY, etc.)
 * @property avgValue Valor promedio de las lecturas en esta hora
 * @property minValue Valor mínimo de las lecturas
 * @property maxValue Valor máximo de las lecturas
 * @property stddevValue Desviación estándar (indicador de calidad de datos)
 * @property countReadings Número de lecturas agregadas en esta hora
 * @property nullCount Número de lecturas NULL
 * @property outOfRangeCount Lecturas fuera de umbrales
 * @property unit Unidad de medida
 * @property firstReadingAt Timestamp de la primera lectura en esta hora
 * @property lastReadingAt Timestamp de la última lectura en esta hora
 * @property createdAt Fecha de creación del registro
 * @property updatedAt Fecha de última actualización
 */
@Entity
@Table(
    name = "sensor_readings_hourly",
    schema = "iot",
    indexes = [
        Index(name = "idx_hourly_greenhouse_time", columnList = "greenhouse_id, time"),
        Index(name = "idx_hourly_tenant_time", columnList = "tenant_id, time"),
        Index(name = "idx_hourly_greenhouse_sensor_type", columnList = "greenhouse_id, sensor_type, time"),
        Index(name = "idx_hourly_tenant_sensor_type", columnList = "tenant_id, sensor_type, time"),
        Index(name = "idx_hourly_composite", columnList = "tenant_id, greenhouse_id, sensor_type, time")
    ]
)
@IdClass(SensorReadingsHourlyId::class)
data class SensorReadingsHourly(
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

    // Quality metrics
    @Column(name = "null_count")
    val nullCount: Long? = 0,

    @Column(name = "out_of_range_count")
    val outOfRangeCount: Long? = 0,

    // Metadata
    @Column(length = 20)
    val unit: String? = null,

    @Column(name = "first_reading_at")
    val firstReadingAt: Instant? = null,

    @Column(name = "last_reading_at")
    val lastReadingAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

package com.apptolast.invernaderos.entities.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Clase de ID compuesta para SensorReading
 * Necesaria para la clave primaria compuesta (time, sensorId)
 */
data class SensorReadingId(
    val time: Instant = Instant.now(),
    val sensorId: String = ""
) : Serializable

/**
 * Entidad para almacenar lecturas de sensores en TimescaleDB
 *
 * Usa una clave primaria compuesta (time, sensorId) para permitir
 * múltiples lecturas de diferentes sensores con el mismo timestamp
 *
 * @property time Timestamp de la lectura
 * @property sensorId ID único del sensor (parte de clave primaria compuesta)
 * @property greenhouseId UUID del invernadero (NOT NULL después de migración V8)
 * @property tenantId UUID del tenant (denormalizado para queries multi-tenant optimizados)
 * @property sensorType Tipo de sensor (TEMPERATURE, HUMIDITY, etc.)
 * @property value Valor numérico de la lectura
 * @property unit Unidad de medida (opcional)
 */
@Entity
@Table(name = "sensor_readings", schema = "iot")
@IdClass(SensorReadingId::class)
data class SensorReading(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Id
    @Column(name = "sensor_id", nullable = false, length = 50)
    val sensorId: String,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "tenant_id")
    val tenantId: UUID? = null,

    @Column(name = "sensor_type", nullable = false, length = 30)
    val sensorType: String,

    @Column(nullable = false, columnDefinition = "double precision")
    val value: Double,

    @Column(length = 20)
    val unit: String? = null
)

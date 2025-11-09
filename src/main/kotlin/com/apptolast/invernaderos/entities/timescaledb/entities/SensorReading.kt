package com.apptolast.invernaderos.entities.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

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
 */
@Entity
@Table(name = "sensor_readings", schema = "public")
@IdClass(SensorReadingId::class)
data class SensorReading(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Id
    @Column(name = "sensor_id", nullable = false, length = 50)
    val sensorId: String,

    @Column(name = "greenhouse_id", length = 50)
    val greenhouseId: String? = null,

    @Column(name = "sensor_type", nullable = false, length = 30)
    val sensorType: String,

    @Column(nullable = false, columnDefinition = "double precision")
    val value: Double,

    @Column(length = 20)
    val unit: String? = null
)

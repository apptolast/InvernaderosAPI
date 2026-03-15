package com.apptolast.invernaderos.features.telemetry.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

/**
 * Clase de ID compuesta para SensorReading
 * Clave primaria compuesta (time, code)
 */
data class SensorReadingId(
    val time: Instant = Instant.now(),
    val code: String = ""
) : Serializable

/**
 * Entidad para almacenar lecturas en TimescaleDB
 *
 * Recibe datos del topic MQTT GREENHOUSE/STATUS con formato:
 * {"id":"SET-00036","value":15} o {"id":"DEV-00031","value":false}
 *
 * El campo 'code' enlaza con metadata.settings.code o metadata.devices.code
 *
 * @property time Timestamp de la lectura
 * @property code Código del device o setting (e.g., SET-00036, DEV-00031)
 * @property value Valor como string para soportar todos los tipos de datos
 */
@Entity
@Table(name = "sensor_readings", schema = "iot")
@IdClass(SensorReadingId::class)
data class SensorReading(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Id
    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 100)
    val value: String
)

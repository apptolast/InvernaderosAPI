package com.apptolast.invernaderos.features.telemetry.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Clase de ID compuesta para Reading
 * Necesaria para la clave primaria compuesta (time, deviceId)
 */
data class ReadingId(
    val time: Instant = Instant.now(),
    val deviceId: UUID = UUID.randomUUID()
) : Serializable

/**
 * Entidad simplificada para almacenar lecturas de dispositivos IoT en TimescaleDB.
 *
 * Esta es la nueva tabla readings que reemplaza sensor_readings con una estructura mas simple:
 * - Solo 4 campos: time, device_id, value, metadata
 * - La informacion adicional (tipo de sensor, unidad, etc.) se obtiene del dispositivo o se guarda en metadata
 *
 * @property time Timestamp de la lectura (parte de PK)
 * @property deviceId UUID del dispositivo que genero la lectura (parte de PK)
 * @property value Valor numerico de la lectura
 * @property metadata Datos adicionales en formato JSONB (sensor_id legacy, sensor_type, unit, etc.)
 */
@Entity
@Table(name = "readings", schema = "iot")
@IdClass(ReadingId::class)
data class Reading(
    @Id
    @Column(name = "time", nullable = false)
    val time: Instant,

    @Id
    @Column(name = "device_id", nullable = false)
    val deviceId: UUID,

    @Column(name = "value", nullable = false, columnDefinition = "double precision")
    val value: Double,

    @Column(name = "metadata", columnDefinition = "jsonb")
    val metadata: String? = "{}"
) {
    override fun toString(): String {
        return "Reading(time=$time, deviceId=$deviceId, value=$value)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Reading) return false
        return time == other.time && deviceId == other.deviceId
    }

    override fun hashCode(): Int {
        var result = time.hashCode()
        result = 31 * result + deviceId.hashCode()
        return result
    }
}

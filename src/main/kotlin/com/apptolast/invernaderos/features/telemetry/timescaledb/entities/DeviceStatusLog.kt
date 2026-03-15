package com.apptolast.invernaderos.features.telemetry.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

/**
 * Clase de ID compuesta para DeviceStatusLog
 * Clave primaria compuesta (time, code) para permitir
 * múltiples lecturas de diferentes dispositivos/settings con el mismo timestamp
 */
data class DeviceStatusLogId(
    val time: Instant = Instant.now(),
    val code: String = ""
) : Serializable

/**
 * Entidad para almacenar el log de estados de dispositivos y settings en TimescaleDB
 *
 * Recibe datos del topic MQTT GREENHOUSE/STATUS con formato:
 * {"id":"SET-00036","value":15} o {"id":"DEV-00031","value":false}
 *
 * El campo 'code' enlaza con:
 * - metadata.settings.code (SET-XXXXX)
 * - metadata.devices.code (DEV-XXXXX)
 *
 * @property time Timestamp de la lectura
 * @property code Código del dispositivo o setting (e.g., SET-00036, DEV-00031)
 * @property value Valor como string para soportar todos los tipos de datos
 */
@Entity
@Table(name = "device_status_log", schema = "iot")
@IdClass(DeviceStatusLogId::class)
data class DeviceStatusLog(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Id
    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 100)
    val value: String
)

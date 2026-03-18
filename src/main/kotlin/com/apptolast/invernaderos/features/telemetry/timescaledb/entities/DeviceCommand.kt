package com.apptolast.invernaderos.features.telemetry.timescaledb.entities

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

/**
 * Clase de ID compuesta para DeviceCommand.
 * Clave primaria compuesta (time, code).
 */
data class DeviceCommandId(
    val time: Instant = Instant.now(),
    val code: String = ""
) : Serializable

/**
 * Comando enviado desde la app movil al PLC via MQTT.
 *
 * Registra el historico de comandos/consignas enviadas al autonomata.
 * El flujo es fire-and-forget: la API publica a MQTT y no espera confirmacion.
 *
 * @property time Timestamp del envio del comando
 * @property code Codigo del device o setting destino (e.g., SET-00036)
 * @property value Valor del comando como string
 */
@Entity
@Table(name = "device_commands", schema = "iot")
@IdClass(DeviceCommandId::class)
data class DeviceCommand(
    @Id
    @Column(nullable = false)
    val time: Instant,

    @Id
    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 100)
    val value: String
)

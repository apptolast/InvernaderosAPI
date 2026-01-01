package com.apptolast.invernaderos.features.device

import com.apptolast.invernaderos.features.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Historico de comandos enviados a actuadores.
 *
 * @property id UUID unico del registro
 * @property deviceId UUID del dispositivo al que se envio el comando
 * @property command Nombre del comando (ej: ON, OFF, SET_VALUE)
 * @property value Valor numerico del comando (si aplica)
 * @property source Origen del comando: USER, SYSTEM, SCHEDULE, ALERT, API, MQTT
 * @property userId UUID del usuario que envio el comando (si fue manual)
 * @property success Si el comando fue exitoso
 * @property response Respuesta del dispositivo en formato JSONB
 * @property createdAt Fecha de creacion del registro
 */
@Entity
@Table(
    name = "command_history",
    schema = "metadata",
    indexes = [
        Index(name = "idx_command_history_device", columnList = "device_id"),
        Index(name = "idx_command_history_created", columnList = "created_at"),
        Index(name = "idx_command_history_device_time", columnList = "device_id, created_at")
    ]
)
data class CommandHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "device_id", nullable = false)
    val deviceId: UUID,

    @Column(nullable = false, length = 50)
    val command: String,

    @Column
    val value: Double? = null,

    @Column(length = 30)
    val source: String? = null,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column
    val success: Boolean? = null,

    @Column(columnDefinition = "jsonb")
    val response: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "device_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var device: Device? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var user: User? = null

    override fun toString(): String {
        return "CommandHistory(id=$id, deviceId=$deviceId, command='$command', value=$value, source=$source, success=$success)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandHistory) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    companion object {
        object Source {
            const val USER = "USER"
            const val SYSTEM = "SYSTEM"
            const val SCHEDULE = "SCHEDULE"
            const val ALERT = "ALERT"
            const val API = "API"
            const val MQTT = "MQTT"
        }
    }
}

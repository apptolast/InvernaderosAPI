package com.apptolast.invernaderos.features.device

import com.apptolast.invernaderos.features.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Origen del comando ejecutado
 */
enum class CommandSource {
    USER,
    SYSTEM,
    SCHEDULE,
    ALERT,
    API,
    MQTT
}

/**
 * Entity que representa el historico de comandos ejecutados en dispositivos (actuadores).
 *
 * @property id UUID unico del registro
 * @property deviceId UUID del dispositivo (actuador)
 * @property command Comando ejecutado (ON, OFF, SET_VALUE, etc.)
 * @property value Valor asociado al comando (opcional)
 * @property source Origen del comando (USER, SYSTEM, SCHEDULE, etc.)
 * @property userId UUID del usuario que ejecuto (si aplica)
 * @property success Si el comando fue exitoso
 * @property response Respuesta del dispositivo en formato JSONB
 * @property createdAt Fecha de creacion
 */
@NamedEntityGraph(
    name = "CommandHistory.context",
    attributeNodes = [NamedAttributeNode("device"), NamedAttributeNode("user")]
)
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
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "device_id", nullable = false)
    val deviceId: UUID,

    @Column(name = "command", length = 50, nullable = false)
    val command: String,

    @Column(name = "value")
    val value: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30)
    val source: CommandSource? = null,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(name = "success")
    val success: Boolean? = null,

    @Column(name = "response", columnDefinition = "jsonb")
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
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

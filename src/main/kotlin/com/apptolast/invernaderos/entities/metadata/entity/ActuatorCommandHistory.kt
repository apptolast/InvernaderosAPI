package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.UUID

/**
 * Entidad READONLY para historial de comandos enviados a actuadores.
 * Registra cada comando (TURN_ON, TURN_OFF, SET_VALUE, etc.) con estado anterior y resultado.
 *
 * ⚠️ IMPORTANTE: Esta es una entidad de solo lectura (@Immutable).
 * Los registros se crean mediante triggers de base de datos.
 *
 * @property id ID autoincremental del registro
 * @property actuatorId UUID del actuador que recibió el comando
 * @property greenhouseId UUID del invernadero (denormalizado)
 * @property tenantId UUID del tenant (denormalizado)
 * @property command Comando enviado: TURN_ON, TURN_OFF, SET_VALUE, SET_AUTO, SET_MANUAL
 * @property targetValue Valor objetivo (para SET_VALUE)
 * @property targetStateId Estado objetivo (referencia a actuator_states)
 * @property previousStateId Estado anterior (para rollback)
 * @property previousValue Valor anterior
 * @property newStateId Estado resultante después del comando
 * @property newValue Valor resultante
 * @property triggeredBy Fuente del comando: USER, AUTOMATION, SCHEDULE, ALERT, API, SYSTEM
 * @property userId UUID del usuario que envió el comando (NULL si automático)
 * @property commandedAt Timestamp del comando
 * @property executedAt Timestamp de ejecución
 * @property durationMs Duración de ejecución en milisegundos
 * @property success Si el comando se ejecutó exitosamente
 * @property errorMessage Mensaje de error si falló
 * @property createdAt Timestamp de creación del registro
 */
@Entity
@Table(name = "actuator_command_history", schema = "metadata")
@Immutable
data class ActuatorCommandHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "actuator_id", nullable = false)
    val actuatorId: UUID,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "command", nullable = false, length = 50)
    val command: String,

    @Column(name = "target_value")
    val targetValue: Double? = null,

    @Column(name = "target_state_id")
    val targetStateId: Short? = null,

    @Column(name = "previous_state_id")
    val previousStateId: Short? = null,

    @Column(name = "previous_value")
    val previousValue: Double? = null,

    @Column(name = "new_state_id")
    val newStateId: Short? = null,

    @Column(name = "new_value")
    val newValue: Double? = null,

    @Column(name = "triggered_by", nullable = false, length = 20)
    val triggeredBy: String,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(name = "commanded_at", nullable = false)
    val commandedAt: Instant,

    @Column(name = "executed_at")
    val executedAt: Instant? = null,

    @Column(name = "duration_ms")
    val durationMs: Int? = null,

    @Column(name = "success", nullable = false)
    val success: Boolean = false,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActuatorCommandHistory) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String {
        return "ActuatorCommandHistory(id=$id, actuatorId=$actuatorId, command='$command', success=$success, commandedAt=$commandedAt)"
    }
}

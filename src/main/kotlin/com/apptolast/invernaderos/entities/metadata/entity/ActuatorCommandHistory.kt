package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Entity para registrar el historial de comandos enviados a actuadores.
 * Permite auditar todas las acciones de control sobre los actuadores del invernadero.
 *
 * @property id ID único del registro (BIGSERIAL)
 * @property actuatorId UUID del actuador que recibió el comando
 * @property greenhouseId UUID del invernadero
 * @property tenantId UUID del tenant
 * @property triggeredByUserId UUID del usuario que disparó el comando (si fue manual)
 * @property targetStateId ID del estado objetivo (SMALLINT)
 * @property previousStateId ID del estado anterior (SMALLINT)
 * @property newStateId ID del nuevo estado después del comando (SMALLINT)
 * @property command Comando enviado (ej: "ON", "OFF", "SET_SPEED", "AUTO")
 * @property targetValue Valor objetivo del comando (para actuadores continuos)
 * @property previousValue Valor anterior
 * @property newValue Nuevo valor después del comando
 * @property triggeredBy Origen del comando: USER, AUTOMATION, SCHEDULE, ALERT, API, SYSTEM
 * @property triggeredByRuleId UUID de la regla de automatización que disparó el comando
 * @property commandSentAt Timestamp cuando se envió el comando
 * @property commandExecutedAt Timestamp cuando el actuador ejecutó el comando
 * @property executionStatus Estado de ejecución: PENDING, SENT, ACKNOWLEDGED, EXECUTED, FAILED, TIMEOUT
 * @property errorMessage Mensaje de error si el comando falló
 * @property metadata Metadata adicional en formato JSONB
 * @property createdAt Fecha de creación del registro
 */
@Entity
@Table(
    name = "actuator_command_history",
    schema = "metadata",
    indexes = [
        Index(name = "idx_actuator_cmd_history_actuator", columnList = "actuator_id"),
        Index(name = "idx_actuator_cmd_history_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_actuator_cmd_history_tenant", columnList = "tenant_id"),
        Index(name = "idx_actuator_cmd_history_user", columnList = "triggered_by_user_id"),
        Index(name = "idx_actuator_cmd_history_sent_at", columnList = "command_sent_at"),
        Index(name = "idx_actuator_cmd_history_triggered_by", columnList = "triggered_by"),
        Index(name = "idx_actuator_cmd_history_status", columnList = "execution_status")
    ]
)
data class ActuatorCommandHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "actuator_id", nullable = false)
    val actuatorId: UUID,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "triggered_by_user_id")
    val triggeredByUserId: UUID? = null,

    @Column(name = "target_state_id", columnDefinition = "SMALLINT")
    val targetStateId: Short? = null,

    @Column(name = "previous_state_id", columnDefinition = "SMALLINT")
    val previousStateId: Short? = null,

    @Column(name = "new_state_id", columnDefinition = "SMALLINT")
    val newStateId: Short? = null,

    @Column(nullable = false, length = 50)
    val command: String,

    /**
     * Valor objetivo para actuadores continuos (ej: velocidad 0-100%, temperatura objetivo).
     */
    @Column(name = "target_value")
    val targetValue: Double? = null,

    @Column(name = "previous_value")
    val previousValue: Double? = null,

    @Column(name = "new_value")
    val newValue: Double? = null,

    /**
     * Origen del comando:
     * - USER: Comando manual por usuario
     * - AUTOMATION: Disparado por regla de automatización
     * - SCHEDULE: Disparado por programación
     * - ALERT: Disparado por alerta
     * - API: Enviado vía API externa
     * - SYSTEM: Comando del sistema
     */
    @Column(name = "triggered_by", nullable = false, length = 20)
    val triggeredBy: String,

    /**
     * UUID de la regla de automatización que disparó el comando (si aplica).
     */
    @Column(name = "triggered_by_rule_id")
    val triggeredByRuleId: UUID? = null,

    @Column(name = "command_sent_at", nullable = false)
    val commandSentAt: Instant = Instant.now(),

    /**
     * Timestamp cuando el actuador confirmó la ejecución del comando.
     */
    @Column(name = "command_executed_at")
    val commandExecutedAt: Instant? = null,

    /**
     * Estado de ejecución del comando:
     * - PENDING: Comando creado pero no enviado
     * - SENT: Comando enviado al actuador
     * - ACKNOWLEDGED: Actuador confirmó recepción
     * - EXECUTED: Actuador ejecutó el comando exitosamente
     * - FAILED: Comando falló
     * - TIMEOUT: Comando expiró sin respuesta
     */
    @Column(name = "execution_status", nullable = false, length = 20)
    val executionStatus: String = ExecutionStatus.PENDING,

    /**
     * Mensaje de error si el comando falló.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    /**
     * Metadata adicional del comando en formato JSONB.
     * Ejemplo: {"mqtt_topic": "...", "qos": 1, "retry_count": 0}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val metadata: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con Actuator.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actuator_id", referencedColumnName = "id", insertable = false, updatable = false)
    var actuator: Actuator? = null

    /**
     * Relación ManyToOne con Greenhouse.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenhouse_id", referencedColumnName = "id", insertable = false, updatable = false)
    var greenhouse: Greenhouse? = null

    /**
     * Relación ManyToOne con Tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    var tenant: Tenant? = null

    /**
     * Relación ManyToOne con User (usuario que disparó el comando).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id", referencedColumnName = "id", insertable = false, updatable = false)
    var user: User? = null

    override fun toString(): String {
        return "ActuatorCommandHistory(id=$id, actuatorId=$actuatorId, command='$command', triggeredBy='$triggeredBy', executionStatus='$executionStatus', commandSentAt=$commandSentAt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActuatorCommandHistory) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object TriggeredBy {
            const val USER = "USER"
            const val AUTOMATION = "AUTOMATION"
            const val SCHEDULE = "SCHEDULE"
            const val ALERT = "ALERT"
            const val API = "API"
            const val SYSTEM = "SYSTEM"
        }

        object ExecutionStatus {
            const val PENDING = "PENDING"
            const val SENT = "SENT"
            const val ACKNOWLEDGED = "ACKNOWLEDGED"
            const val EXECUTED = "EXECUTED"
            const val FAILED = "FAILED"
            const val TIMEOUT = "TIMEOUT"
        }
    }
}

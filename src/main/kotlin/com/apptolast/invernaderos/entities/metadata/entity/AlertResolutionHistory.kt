package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.UUID

/**
 * Entidad READONLY para auditoría de resolución de alertas.
 * Registra QUIÉN, CUÁNDO y CÓMO se resolvió cada alerta.
 *
 * ⚠️ IMPORTANTE: Esta es una entidad de solo lectura (@Immutable).
 * Los registros se crean mediante triggers de base de datos o SQL directo.
 *
 * @property id UUID único del registro de resolución
 * @property alertId UUID de la alerta que fue resuelta
 * @property resolvedBy UUID del usuario que resolvió la alerta
 * @property resolvedAt Timestamp de cuando se resolvió
 * @property resolutionNotes Notas sobre cómo se resolvió
 * @property actionsTaken Array de acciones tomadas (ej: ['SENSOR_RECALIBRATED', 'ACTUATOR_REPAIRED'])
 * @property wasActuatorModified Si se modificó algún actuador durante la resolución
 * @property wasSensorRecalibrated Si se recalibró algún sensor
 * @property timeToResolveMinutes Tiempo que tomó resolver la alerta (en minutos)
 * @property createdAt Timestamp de creación del registro (para auditoría)
 */
@Entity
@Table(name = "alert_resolution_history", schema = "metadata")
@Immutable
data class AlertResolutionHistory(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,

    @Column(name = "alert_id", nullable = false)
    val alertId: UUID,

    @Column(name = "resolved_by", nullable = false)
    val resolvedBy: UUID,

    @Column(name = "resolved_at", nullable = false)
    val resolvedAt: Instant,

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    val resolutionNotes: String? = null,

    /**
     * Array PostgreSQL de acciones tomadas.
     * Hibernate mapea text[] a Array<String> en Kotlin.
     * Ejemplo: ['SENSOR_RECALIBRATED', 'ACTUATOR_REPAIRED', 'THRESHOLD_ADJUSTED']
     */
    @Column(name = "actions_taken", columnDefinition = "text[]")
    val actionsTaken: Array<String>? = null,

    @Column(name = "was_actuator_modified", nullable = false)
    val wasActuatorModified: Boolean = false,

    @Column(name = "was_sensor_recalibrated", nullable = false)
    val wasSensorRecalibrated: Boolean = false,

    @Column(name = "time_to_resolve_minutes")
    val timeToResolveMinutes: Int? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlertResolutionHistory) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "AlertResolutionHistory(id=$id, alertId=$alertId, resolvedBy=$resolvedBy, resolvedAt=$resolvedAt)"
    }
}

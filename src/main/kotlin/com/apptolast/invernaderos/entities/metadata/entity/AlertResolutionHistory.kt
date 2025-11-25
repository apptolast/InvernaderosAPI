package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Entity para registrar el historial de resoluciones de alertas.
 * Permite rastrear quién, cuándo y cómo se resolvieron las alertas del sistema.
 *
 * @property id ID único del registro (BIGSERIAL)
 * @property alertId UUID de la alerta resuelta
 * @property resolvedBy UUID del usuario que resolvió la alerta
 * @property resolvedAt Timestamp de resolución
 * @property resolutionMethod Método de resolución: MANUAL, AUTOMATIC, SYSTEM, IGNORED
 * @property resolutionNotes Notas sobre la resolución
 * @property actionsTaken Acciones tomadas para resolver la alerta
 * @property timeToResolveSeconds Tiempo transcurrido desde la creación de la alerta hasta su resolución
 * @property wasEscalated Si la alerta fue escalada antes de resolverse
 * @property escalatedTo UUID del usuario al que se escaló (si aplica)
 * @property createdAt Fecha de creación del registro
 */
@Entity
@Table(
    name = "alert_resolution_history",
    schema = "metadata",
    indexes = [
        Index(name = "idx_alert_resolution_history_alert", columnList = "alert_id"),
        Index(name = "idx_alert_resolution_history_resolved_by", columnList = "resolved_by"),
        Index(name = "idx_alert_resolution_history_resolved_at", columnList = "resolved_at"),
        Index(name = "idx_alert_resolution_history_method", columnList = "resolution_method")
    ]
)
data class AlertResolutionHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "alert_id", nullable = false)
    val alertId: UUID,

    @Column(name = "resolved_by", nullable = false)
    val resolvedBy: UUID,

    @Column(name = "resolved_at", nullable = false)
    val resolvedAt: Instant = Instant.now(),

    /**
     * Método de resolución:
     * - MANUAL: Resuelta manualmente por usuario
     * - AUTOMATIC: Resuelta automáticamente por el sistema
     * - SYSTEM: Resuelta por proceso del sistema
     * - IGNORED: Marcada como falsa alarma o no relevante
     */
    @Column(name = "resolution_method", nullable = false, length = 20)
    val resolutionMethod: String,

    /**
     * Notas detalladas sobre cómo se resolvió la alerta.
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    val resolutionNotes: String? = null,

    /**
     * Descripción de las acciones tomadas para resolver la alerta.
     * Ejemplo: "Se reinició el sensor TEMP01 y se verificó su calibración"
     */
    @Column(name = "actions_taken", columnDefinition = "TEXT")
    val actionsTaken: String? = null,

    /**
     * Tiempo transcurrido en segundos desde la creación de la alerta hasta su resolución.
     * Útil para métricas de tiempo de respuesta.
     */
    @Column(name = "time_to_resolve_seconds")
    val timeToResolveSeconds: Int? = null,

    /**
     * Indica si la alerta fue escalada a un nivel superior antes de resolverse.
     */
    @Column(name = "was_escalated", nullable = false)
    val wasEscalated: Boolean = false,

    /**
     * UUID del usuario al que se escaló la alerta (si aplica).
     */
    @Column(name = "escalated_to")
    val escalatedTo: UUID? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con Alert.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", referencedColumnName = "id", insertable = false, updatable = false)
    var alert: Alert? = null

    /**
     * Relación ManyToOne con User (usuario que resolvió la alerta).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by", referencedColumnName = "id", insertable = false, updatable = false)
    var resolvedByUser: User? = null

    /**
     * Relación ManyToOne con User (usuario al que se escaló).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_to", referencedColumnName = "id", insertable = false, updatable = false)
    var escalatedToUser: User? = null

    override fun toString(): String {
        return "AlertResolutionHistory(id=$id, alertId=$alertId, resolutionMethod='$resolutionMethod', resolvedAt=$resolvedAt, timeToResolveSeconds=$timeToResolveSeconds)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlertResolutionHistory) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object ResolutionMethod {
            const val MANUAL = "MANUAL"
            const val AUTOMATIC = "AUTOMATIC"
            const val SYSTEM = "SYSTEM"
            const val IGNORED = "IGNORED"
        }
    }
}

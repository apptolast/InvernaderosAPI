package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Catalogo de niveles de severidad para alertas.
 *
 * @property id ID unico de la severidad (smallint)
 * @property name Nombre de la severidad (ej: INFO, WARNING, ERROR, CRITICAL)
 * @property level Nivel numerico para ordenacion (1=mas bajo, 5=mas alto)
 * @property description Descripcion de la severidad
 * @property color Color hexadecimal para UI
 * @property requiresAction Si requiere accion inmediata
 * @property notificationDelayMinutes Minutos de retraso antes de notificar
 * @property createdAt Fecha de creacion
 */
@Entity
@Table(name = "alert_severities", schema = "metadata")
data class AlertSeverity(
    @Id
    val id: Short,

    @Column(nullable = false, length = 20, unique = true)
    val name: String,

    @Column(nullable = false)
    val level: Short,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(length = 7)
    val color: String? = null,

    @Column(name = "requires_action")
    val requiresAction: Boolean = false,

    @Column(name = "notification_delay_minutes")
    val notificationDelayMinutes: Int = 0,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
) {
    override fun toString(): String {
        return "AlertSeverity(id=$id, name='$name', level=$level)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlertSeverity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val INFO: Short = 1
        const val WARNING: Short = 2
        const val ERROR: Short = 3
        const val CRITICAL: Short = 4
    }
}

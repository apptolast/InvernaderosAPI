package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Estados posibles para actuadores.
 *
 * @property id ID unico del estado (smallserial)
 * @property name Nombre del estado (ej: ON, OFF, STARTING)
 * @property description Descripcion del estado
 * @property isOperational TRUE si el actuador esta funcionando en este estado
 * @property displayOrder Orden para mostrar en UI
 * @property color Color hexadecimal para UI (ej: #00FF00)
 * @property createdAt Fecha de creacion
 */
@Entity
@Table(
    name = "actuator_states",
    schema = "metadata",
    indexes = [
        Index(name = "idx_actuator_states_name", columnList = "name"),
        Index(name = "idx_actuator_states_operational", columnList = "is_operational")
    ]
)
data class ActuatorState(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short? = null,

    @Column(nullable = false, length = 20, unique = true)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "is_operational", nullable = false)
    val isOperational: Boolean = false,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Short = 0,

    @Column(length = 7)
    val color: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun toString(): String {
        return "ActuatorState(id=$id, name='$name', isOperational=$isOperational)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActuatorState) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*

/**
 * Catalogo de tipos de alertas.
 *
 * @property id ID unico del tipo de alerta (smallserial, auto-generado)
 * @property name Nombre del tipo (ej: THRESHOLD_EXCEEDED, SENSOR_OFFLINE)
 * @property description Descripcion del tipo de alerta
 *
 * @see <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html">Spring Data JPA Entity Persistence</a>
 */
@Entity
@Table(name = "alert_types", schema = "metadata")
data class AlertType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short? = null,

    @Column(nullable = false, length = 30, unique = true)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null
) {
    override fun toString(): String {
        return "AlertType(id=$id, name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlertType) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    companion object {
        const val THRESHOLD_EXCEEDED: Short = 1
        const val SENSOR_OFFLINE: Short = 2
        const val ACTUATOR_FAILURE: Short = 3
        const val SYSTEM_ERROR: Short = 4
    }
}

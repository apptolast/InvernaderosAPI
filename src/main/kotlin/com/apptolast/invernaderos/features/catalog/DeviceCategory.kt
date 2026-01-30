package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*

/**
 * Categorias de dispositivos (SENSOR, ACTUATOR).
 *
 * @property id ID unico de la categoria (smallserial, auto-generado)
 * @property name Nombre de la categoria
 *
 * @see <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html">Spring Data JPA Entity Persistence</a>
 */
@Entity
@Table(name = "device_categories", schema = "metadata")
data class DeviceCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short? = null,

    @Column(nullable = false, length = 20, unique = true)
    val name: String
) {
    override fun toString(): String {
        return "DeviceCategory(id=$id, name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceCategory) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    companion object {
        const val SENSOR: Short = 1
        const val ACTUATOR: Short = 2
    }
}

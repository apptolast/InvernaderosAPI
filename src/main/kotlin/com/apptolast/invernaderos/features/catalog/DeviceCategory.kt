package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*

/**
 * Categorias de dispositivos (SENSOR, ACTUATOR).
 *
 * @property id ID unico de la categoria (smallint)
 * @property name Nombre de la categoria
 */
@Entity
@Table(name = "device_categories", schema = "metadata")
data class DeviceCategory(
    @Id
    val id: Short,

    @Column(nullable = false, length = 20, unique = true)
    val name: String
) {
    override fun toString(): String {
        return "DeviceCategory(id=$id, name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceCategory) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val SENSOR: Short = 1
        const val ACTUATOR: Short = 2
    }
}

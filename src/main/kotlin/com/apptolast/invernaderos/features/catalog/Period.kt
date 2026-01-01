package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*

/**
 * Catalogo de periodos del dia para configuraciones.
 *
 * @property id ID unico del periodo (smallint)
 * @property name Nombre del periodo (DAY, NIGHT, ALL)
 */
@Entity
@Table(name = "periods", schema = "metadata")
data class Period(
    @Id
    val id: Short,

    @Column(nullable = false, length = 10, unique = true)
    val name: String
) {
    override fun toString(): String {
        return "Period(id=$id, name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Period) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val DAY: Short = 1
        const val NIGHT: Short = 2
        const val ALL: Short = 3
    }
}

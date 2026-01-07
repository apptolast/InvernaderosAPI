package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*

/**
 * Catalogo de periodos del dia para configuraciones.
 *
 * @property id ID unico del periodo (smallserial, auto-generado)
 * @property name Nombre del periodo (DAY, NIGHT, ALL)
 *
 * @see <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html">Spring Data JPA Entity Persistence</a>
 */
@Entity
@Table(name = "periods", schema = "metadata")
data class Period(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short? = null,

    @Column(nullable = false, length = 10, unique = true)
    val name: String
) {
    override fun toString(): String {
        return "Period(id=$id, name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Period) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    companion object {
        const val DAY: Short = 1
        const val NIGHT: Short = 2
        const val ALL: Short = 3
    }
}

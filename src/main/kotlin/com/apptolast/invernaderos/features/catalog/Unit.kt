package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Catalogo de unidades de medida para sensores y actuadores.
 *
 * @property id ID unico de la unidad (smallserial)
 * @property symbol Simbolo de la unidad (ej: C, %, hPa)
 * @property name Nombre descriptivo de la unidad
 * @property description Descripcion opcional
 * @property isActive Si la unidad esta activa
 * @property createdAt Fecha de creacion
 */
@Entity
@Table(
    name = "units",
    schema = "metadata",
    indexes = [
        Index(name = "idx_units_symbol", columnList = "symbol"),
        Index(name = "idx_units_active", columnList = "is_active")
    ]
)
data class Unit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short? = null,

    @Column(nullable = false, length = 10, unique = true)
    val symbol: String,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun toString(): String {
        return "Unit(id=$id, symbol='$symbol', name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Unit) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

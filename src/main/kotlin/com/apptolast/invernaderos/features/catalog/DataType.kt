package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Tipos de datos para configuraciones de Settings.
 * Define los tipos basicos soportados: INTEGER, LONG, DOUBLE, BOOLEAN, STRING, DATE, TIME, DATETIME, JSON
 *
 * @property id ID unico del tipo de dato (smallserial)
 * @property name Nombre del tipo (ej: INTEGER, BOOLEAN, STRING)
 * @property description Descripcion del tipo de dato
 * @property validationRegex Expresion regular para validar valores de este tipo
 * @property exampleValue Ejemplo de valor valido para este tipo
 * @property displayOrder Orden para mostrar en UI
 * @property isActive Si el tipo esta activo
 * @property createdAt Fecha de creacion
 */
@Entity
@Table(
    name = "data_types",
    schema = "metadata",
    indexes = [
        Index(name = "idx_data_types_name", columnList = "name"),
        Index(name = "idx_data_types_active", columnList = "is_active")
    ]
)
data class DataType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short? = null,

    @Column(nullable = false, length = 20, unique = true)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "validation_regex", length = 500)
    val validationRegex: String? = null,

    @Column(name = "example_value", length = 100)
    val exampleValue: String? = null,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Short = 0,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun toString(): String {
        return "DataType(id=$id, name='$name', isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataType) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

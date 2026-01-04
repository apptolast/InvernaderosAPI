package com.apptolast.invernaderos.features.catalog

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Catalogo unificado de tipos de dispositivos (sensores y actuadores).
 *
 * @property id ID unico del tipo (smallserial)
 * @property name Nombre del tipo (ej: TEMPERATURE, HUMIDITY, VALVE)
 * @property description Descripcion del tipo
 * @property defaultUnitId FK a unidad por defecto
 * @property dataType Tipo de dato que genera (DECIMAL, INTEGER, BOOLEAN, TEXT, JSON)
 * @property minExpectedValue Valor minimo fisicamente posible
 * @property maxExpectedValue Valor maximo fisicamente posible
 * @property controlType Tipo de control: BINARY (on/off), CONTINUOUS (0-100%), MULTI_STATE
 * @property isActive Si el tipo esta activo
 * @property createdAt Fecha de creacion
 * @property categoryId FK a categoria de dispositivo
 */
@NamedEntityGraph(
    name = "DeviceType.withRelations",
    attributeNodes = [
        NamedAttributeNode("category"),
        NamedAttributeNode("defaultUnit")
    ]
)
@Entity
@Table(
    name = "device_types",
    schema = "metadata",
    indexes = [
        Index(name = "idx_device_types_name", columnList = "name"),
        Index(name = "idx_device_types_active", columnList = "is_active")
    ]
)
data class DeviceType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Short? = null,

    @Column(nullable = false, length = 30, unique = true)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "default_unit_id")
    val defaultUnitId: Short? = null,

    @Column(name = "data_type", length = 20)
    val dataType: String? = "DECIMAL",

    @Column(name = "min_expected_value", precision = 10, scale = 2)
    val minExpectedValue: BigDecimal? = null,

    @Column(name = "max_expected_value", precision = 10, scale = 2)
    val maxExpectedValue: BigDecimal? = null,

    @Column(name = "control_type", length = 20)
    val controlType: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "category_id", nullable = false)
    val categoryId: Short
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "default_unit_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var defaultUnit: Unit? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "category_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var category: DeviceCategory? = null

    override fun toString(): String {
        return "DeviceType(id=$id, name='$name', categoryId=$categoryId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceType) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    companion object {
        object DataType {
            const val DECIMAL = "DECIMAL"
            const val INTEGER = "INTEGER"
            const val BOOLEAN = "BOOLEAN"
            const val TEXT = "TEXT"
            const val JSON = "JSON"
        }

        object ControlType {
            const val BINARY = "BINARY"
            const val CONTINUOUS = "CONTINUOUS"
            const val MULTI_STATE = "MULTI_STATE"
        }
    }
}

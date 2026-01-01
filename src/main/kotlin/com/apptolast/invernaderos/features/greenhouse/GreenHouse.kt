package com.apptolast.invernaderos.features.greenhouse

import com.apptolast.invernaderos.features.tenant.Tenant
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa un Invernadero en el sistema.
 * Cada greenhouse pertenece a un Tenant y puede tener multiples dispositivos.
 *
 * @property id UUID unico del invernadero
 * @property tenantId UUID del tenant propietario
 * @property name Nombre del invernadero (unico dentro del tenant)
 * @property location Ubicacion en formato JSONB: {lat: number, lon: number}
 * @property areaM2 Area en metros cuadrados
 * @property timezone Zona horaria (default: Europe/Madrid)
 * @property isActive Si el invernadero esta activo
 * @property createdAt Fecha de creacion
 * @property updatedAt Fecha de ultima actualizacion
 */
@NamedEntityGraph(
    name = "Greenhouse.context",
    attributeNodes = [NamedAttributeNode("tenant")]
)
@Entity
@Table(
    name = "greenhouses",
    schema = "metadata",
    indexes = [
        Index(name = "idx_greenhouses_tenant", columnList = "tenant_id"),
        Index(name = "idx_greenhouses_active", columnList = "is_active"),
        Index(name = "idx_greenhouses_tenant_active", columnList = "tenant_id, is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_greenhouse_tenant_name",
            columnNames = ["tenant_id", "name"]
        )
    ]
)
data class Greenhouse(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(nullable = false, length = 100)
    var name: String,

    /**
     * Ubicacion en formato JSONB: {lat: number, lon: number}
     */
    @Column(columnDefinition = "jsonb")
    var location: String? = null,

    @Column(name = "area_m2", precision = 10, scale = 2)
    var areaM2: BigDecimal? = null,

    @Column(length = 50)
    var timezone: String? = "Europe/Madrid",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * Relacion ManyToOne con Tenant.
     * Un invernadero pertenece a un tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "tenant_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var tenant: Tenant? = null

    override fun toString(): String {
        return "Greenhouse(id=$id, name='$name', tenantId=$tenantId, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Greenhouse) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}

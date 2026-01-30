package com.apptolast.invernaderos.features.sector

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import io.hypersistence.utils.hibernate.id.Tsid
import jakarta.persistence.*

/**
 * Subdivisiones logicas de un invernadero para agrupar dispositivos.
 *
 * Jerarquia: Tenant -> Greenhouse -> Sector -> Device
 *
 * @property id ID unico del sector (TSID - Time-Sorted ID, unico global)
 * @property code Codigo unico por tenant (ej: SEC-00001). Generado automaticamente por el backend.
 * @property tenantId ID del tenant propietario
 * @property greenhouseId ID del invernadero al que pertenece
 * @property name Nombre o descripcion del sector
 */
@NamedEntityGraph(
    name = "Sector.withGreenhouse",
    attributeNodes = [
        NamedAttributeNode("greenhouse")
    ]
)
@Entity
@Table(
    name = "sectors",
    schema = "metadata",
    indexes = [
        Index(name = "idx_sectors_tenant", columnList = "tenant_id"),
        Index(name = "idx_sectors_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_sectors_tenant_greenhouse", columnList = "tenant_id, greenhouse_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_sectors_tenant_code", columnNames = ["tenant_id", "code"])
    ]
)
data class Sector(
    @Id
    @Tsid
    var id: Long? = null,

    /**
     * Codigo unico por tenant para identificacion externa.
     * Formato: SEC-{numero_padded} (ej: SEC-00001)
     * Generado automaticamente por el backend. Unico dentro del tenant.
     */
    @Column(nullable = false, length = 50)
    var code: String,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Column(name = "greenhouse_id", nullable = false)
    var greenhouseId: Long,

    @Column(length = 100)
    var name: String? = null
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "tenant_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var tenant: Tenant? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "greenhouse_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var greenhouse: Greenhouse? = null

    override fun toString(): String {
        return "Sector(id=$id, code=$code, tenantId=$tenantId, greenhouseId=$greenhouseId, name=$name)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sector) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

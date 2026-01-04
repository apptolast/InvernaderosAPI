package com.apptolast.invernaderos.features.device

import com.apptolast.invernaderos.features.catalog.DeviceCategory
import com.apptolast.invernaderos.features.catalog.DeviceType
import com.apptolast.invernaderos.features.catalog.Unit
import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Dispositivos IoT unificados (sensores + actuadores).
 *
 * @property id UUID unico del dispositivo
 * @property tenantId UUID del tenant propietario
 * @property greenhouseId UUID del invernadero
 * @property categoryId Categoria: SENSOR o ACTUATOR
 * @property typeId Tipo de dispositivo (temperatura, humedad, valvula, etc.)
 * @property unitId Unidad de medida
 * @property isActive Si el dispositivo esta activo
 * @property createdAt Fecha de creacion
 * @property updatedAt Fecha de ultima actualizacion
 */
@NamedEntityGraph(
    name = "Device.withCatalog",
    attributeNodes = [
        NamedAttributeNode("category"),
        NamedAttributeNode("type"),
        NamedAttributeNode("unit")
    ]
)
@Entity
@Table(
    name = "devices",
    schema = "metadata",
    indexes = [
        Index(name = "idx_devices_new_tenant", columnList = "tenant_id"),
        Index(name = "idx_devices_new_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_devices_new_active", columnList = "is_active")
    ]
)
data class Device(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "category_id")
    val categoryId: Short? = null,

    @Column(name = "type_id")
    val typeId: Short? = null,

    @Column(name = "unit_id")
    val unitId: Short? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "category_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var category: DeviceCategory? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "type_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var type: DeviceType? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "unit_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var unit: Unit? = null

    override fun toString(): String {
        return "Device(id=$id, tenantId=$tenantId, greenhouseId=$greenhouseId, categoryId=$categoryId, typeId=$typeId, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Device) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

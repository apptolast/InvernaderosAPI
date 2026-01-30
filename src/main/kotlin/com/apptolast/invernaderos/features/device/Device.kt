package com.apptolast.invernaderos.features.device

import com.apptolast.invernaderos.features.catalog.DeviceCategory
import com.apptolast.invernaderos.features.catalog.DeviceType
import com.apptolast.invernaderos.features.catalog.Unit
import com.apptolast.invernaderos.features.sector.Sector
import com.apptolast.invernaderos.features.tenant.Tenant
import io.hypersistence.utils.hibernate.id.Tsid
import jakarta.persistence.*
import java.time.Instant

/**
 * Dispositivos IoT unificados (sensores + actuadores).
 *
 * Jerarquia: Tenant -> Greenhouse -> Sector -> Device
 *
 * @property id ID unico del dispositivo (TSID - Time-Sorted ID, unico global)
 * @property code Codigo unico por tenant (ej: DEV-00001). Generado automaticamente por el backend.
 * @property tenantId ID del tenant propietario
 * @property sectorId ID del sector al que pertenece el dispositivo
 * @property name Nombre legible del dispositivo (ej: "Sensor Temperatura Invernadero 1")
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
        NamedAttributeNode("unit"),
        NamedAttributeNode("sector")
    ]
)
@Entity
@Table(
    name = "devices",
    schema = "metadata",
    indexes = [
        Index(name = "idx_devices_tenant", columnList = "tenant_id"),
        Index(name = "idx_devices_sector", columnList = "sector_id"),
        Index(name = "idx_devices_active", columnList = "is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_devices_tenant_code", columnNames = ["tenant_id", "code"])
    ]
)
data class Device(
    @Id
    @Tsid
    var id: Long? = null,

    /**
     * Codigo unico por tenant para identificacion externa.
     * Formato: DEV-{numero_padded} (ej: DEV-00001)
     * Generado automaticamente por el backend. Unico dentro del tenant.
     */
    @Column(nullable = false, length = 50)
    var code: String,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Column(name = "sector_id", nullable = false)
    val sectorId: Long,

    @Column(name = "name", length = 100)
    val name: String? = null,

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
        name = "sector_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var sector: Sector? = null

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
        return "Device(id=$id, code=$code, name=$name, tenantId=$tenantId, sectorId=$sectorId, categoryId=$categoryId, typeId=$typeId, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Device) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

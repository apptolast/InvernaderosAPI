package com.apptolast.invernaderos.features.setting

import com.apptolast.invernaderos.features.catalog.ActuatorState
import com.apptolast.invernaderos.features.catalog.DataType
import com.apptolast.invernaderos.features.catalog.DeviceType
import com.apptolast.invernaderos.features.sector.Sector
import com.apptolast.invernaderos.features.tenant.Tenant
import io.hypersistence.utils.hibernate.id.Tsid
import jakarta.persistence.*
import java.time.Instant

/**
 * Configuraciones de parametros para un sector.
 * Reemplaza a la entidad Setpoint.
 *
 * @property id ID unico de la configuracion (TSID - Time-Sorted ID, unico global)
 * @property code Codigo unico legible para identificacion externa (ej: SET-00001)
 * @property sectorId ID del sector
 * @property tenantId ID del tenant propietario
 * @property parameterId FK al tipo de parametro (temperatura, humedad, etc.)
 * @property actuatorStateId FK al estado del actuador (ON, OFF, AUTO, etc.)
 * @property dataTypeId FK al tipo de dato (INTEGER, BOOLEAN, STRING, etc.)
 * @property value Valor de la configuracion (almacenado como String)
 * @property isActive Si la configuracion esta activa
 * @property createdAt Fecha de creacion
 * @property updatedAt Fecha de ultima actualizacion
 */
@NamedEntityGraph(
    name = "Setting.withCatalog",
    attributeNodes = [
        NamedAttributeNode("parameter"),
        NamedAttributeNode("actuatorState"),
        NamedAttributeNode("dataType")
    ]
)
@Entity
@Table(
    name = "settings",
    schema = "metadata",
    indexes = [
        Index(name = "idx_settings_code", columnList = "code"),
        Index(name = "idx_settings_sector", columnList = "sector_id"),
        Index(name = "idx_settings_actuator_state", columnList = "actuator_state_id"),
        Index(name = "idx_settings_data_type", columnList = "data_type_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_settings_sector_parameter_actuator_state",
            columnNames = ["sector_id", "parameter_id", "actuator_state_id"]
        )
    ]
)
data class Setting(
    @Id
    @Tsid
    var id: Long? = null,

    /**
     * Codigo unico legible para identificacion externa.
     * Formato: SET-{numero_padded} (ej: SET-00001)
     * Usado por PLCs, APIs externas y para debuggear.
     */
    @Column(nullable = false, length = 50, unique = true)
    var code: String,

    @Column(name = "sector_id", nullable = false)
    val sectorId: Long,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    @Column(name = "parameter_id", nullable = false)
    val parameterId: Short,

    @Column(name = "actuator_state_id")
    val actuatorStateId: Short? = null,

    @Column(name = "data_type_id")
    val dataTypeId: Short? = null,

    @Column(name = "value", length = 500)
    val value: String? = null,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
) {
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
        name = "tenant_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var tenant: Tenant? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "parameter_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var parameter: DeviceType? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "actuator_state_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var actuatorState: ActuatorState? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "data_type_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var dataType: DataType? = null

    override fun toString(): String {
        return "Setting(id=$id, sectorId=$sectorId, parameterId=$parameterId, actuatorStateId=$actuatorStateId, dataTypeId=$dataTypeId, value=$value)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Setting) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

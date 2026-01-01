package com.apptolast.invernaderos.features.setting

import com.apptolast.invernaderos.features.catalog.DeviceType
import com.apptolast.invernaderos.features.catalog.Period
import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Configuraciones de parametros para un invernadero.
 * Reemplaza a la entidad Setpoint.
 *
 * @property id UUID unico de la configuracion
 * @property greenhouseId UUID del invernadero
 * @property tenantId UUID del tenant propietario
 * @property parameterId FK al tipo de parametro (temperatura, humedad, etc.)
 * @property periodId FK al periodo (DAY, NIGHT, ALL)
 * @property minValue Valor minimo del rango
 * @property maxValue Valor maximo del rango
 * @property isActive Si la configuracion esta activa
 * @property createdAt Fecha de creacion
 * @property updatedAt Fecha de ultima actualizacion
 */
@Entity
@Table(
    name = "settings",
    schema = "metadata",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_setting_greenhouse_parameter_period",
            columnNames = ["greenhouse_id", "parameter_id", "period_id"]
        )
    ]
)
data class Setting(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "parameter_id", nullable = false)
    val parameterId: Short,

    @Column(name = "period_id", nullable = false)
    val periodId: Short,

    @Column(name = "min_value", precision = 10, scale = 2)
    val minValue: BigDecimal? = null,

    @Column(name = "max_value", precision = 10, scale = 2)
    val maxValue: BigDecimal? = null,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
) {
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
        name = "period_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var period: Period? = null

    override fun toString(): String {
        return "Setting(id=$id, greenhouseId=$greenhouseId, parameterId=$parameterId, periodId=$periodId, minValue=$minValue, maxValue=$maxValue)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Setting) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

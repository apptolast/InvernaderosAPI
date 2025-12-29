package com.apptolast.invernaderos.features.setpoint

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Periodo del dia para aplicar el setpoint
 */
enum class SetpointPeriod {
    DAY,
    NIGHT,
    ALL
}

/**
 * Parametro que controla el setpoint
 */
enum class SetpointParameter {
    TEMPERATURE,
    HUMIDITY,
    CO2,
    LIGHT,
    SOIL_MOISTURE,
    PRESSURE,
    PH,
    EC
}

/**
 * Entity que representa un Setpoint (consigna) para un invernadero.
 * Define los valores objetivo para diferentes parametros ambientales.
 *
 * @property id UUID unico del setpoint
 * @property tenantId UUID del tenant propietario
 * @property greenhouseId UUID del invernadero
 * @property sector Codigo del sector (NULL = todo el invernadero)
 * @property parameter Parametro a controlar (TEMPERATURE, HUMIDITY, etc.)
 * @property period Periodo del dia (DAY, NIGHT, ALL)
 * @property minValue Valor minimo del rango
 * @property maxValue Valor maximo del rango
 * @property targetValue Valor objetivo
 * @property times Horarios en formato JSONB {"start": "06:00", "end": "20:00"}
 * @property isActive Si esta activo
 * @property createdAt Fecha de creacion
 * @property updatedAt Fecha de actualizacion
 */
@NamedEntityGraph(
    name = "Setpoint.context",
    attributeNodes = [NamedAttributeNode("greenhouse"), NamedAttributeNode("tenant")]
)
@Entity
@Table(
    name = "setpoints",
    schema = "metadata",
    indexes = [
        Index(name = "idx_setpoints_tenant", columnList = "tenant_id"),
        Index(name = "idx_setpoints_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_setpoints_parameter", columnList = "parameter"),
        Index(name = "idx_setpoints_active", columnList = "greenhouse_id, is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_setpoint", columnNames = ["greenhouse_id", "sector", "parameter", "period"])
    ]
)
data class Setpoint(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "sector", length = 50)
    val sector: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "parameter", length = 30, nullable = false)
    val parameter: SetpointParameter,

    @Enumerated(EnumType.STRING)
    @Column(name = "period", length = 20, nullable = false)
    val period: SetpointPeriod,

    @Column(name = "min_value", precision = 10, scale = 2)
    val minValue: BigDecimal? = null,

    @Column(name = "max_value", precision = 10, scale = 2)
    val maxValue: BigDecimal? = null,

    @Column(name = "target_value", precision = 10, scale = 2)
    val targetValue: BigDecimal? = null,

    @Column(name = "times", columnDefinition = "jsonb")
    val times: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
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

    override fun toString(): String {
        return "Setpoint(id=$id, parameter=$parameter, period=$period, minValue=$minValue, maxValue=$maxValue, targetValue=$targetValue, greenhouseId=$greenhouseId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Setpoint) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

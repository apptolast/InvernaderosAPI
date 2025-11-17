package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa un Invernadero en el sistema.
 * Cada greenhouse pertenece a un Tenant y puede tener múltiples sensores y actuadores.
 *
 * @property id UUID único del invernadero
 * @property tenantId UUID del tenant propietario
 * @property name Nombre del invernadero
 * @property greenhouseCode Código corto único del invernadero dentro del tenant (ej: INV01, SARA_01)
 * @property mqttTopic Topic MQTT completo asignado (ej: GREENHOUSE/empresa001/inv01)
 * @property mqttPublishIntervalSeconds Intervalo de publicación de datos en segundos
 * @property externalId ID externo del sistema de sensores (si aplica)
 * @property location Ubicación en formato JSONB
 * @property areaM2 Área en metros cuadrados
 * @property cropType Tipo de cultivo
 * @property timezone Zona horaria
 * @property isActive Si el invernadero está activo
 * @property createdAt Fecha de creación
 * @property updatedAt Fecha de última actualización
 */
@Entity
@Table(
    name = "greenhouses",
    schema = "metadata",
    indexes = [
        Index(name = "idx_greenhouses_tenant", columnList = "tenant_id"),
        Index(name = "idx_greenhouses_code", columnList = "greenhouse_code"),
        Index(name = "idx_greenhouses_mqtt_topic", columnList = "mqtt_topic"),
        Index(name = "idx_greenhouses_tenant_active", columnList = "tenant_id, is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_greenhouse_code_per_tenant", columnNames = ["tenant_id", "greenhouse_code"]),
        UniqueConstraint(name = "uq_greenhouse_mqtt_topic", columnNames = ["mqtt_topic"])
    ]
)
data class Greenhouse(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(nullable = false, length = 100)
    val name: String,

    /**
     * Código corto único del invernadero dentro del tenant.
     * Ejemplo: "INV01", "SARA_01", "GREENHOUSE_A"
     * Se usa para identificación rápida y routing MQTT.
     */
    @Column(name = "greenhouse_code", length = 50)
    val greenhouseCode: String? = null,

    /**
     * Topic MQTT completo asignado a este invernadero.
     * Ejemplo: "GREENHOUSE/empresa001", "GREENHOUSE/SARA/inv01"
     * Debe ser único globalmente en el sistema.
     */
    @Column(name = "mqtt_topic", length = 100, unique = true)
    val mqttTopic: String? = null,

    /**
     * Intervalo de publicación de datos del invernadero en segundos.
     * Default: 5 segundos
     */
    @Column(name = "mqtt_publish_interval_seconds")
    val mqttPublishIntervalSeconds: Int? = 5,

    /**
     * ID externo del sistema de sensores o hardware.
     * Útil para integración con sistemas legacy.
     */
    @Column(name = "external_id", length = 100)
    val externalId: String? = null,

    @Column(columnDefinition = "jsonb")
    val location: String? = null,

    @Column(name = "area_m2", precision = 10, scale = 2)
    val areaM2: BigDecimal? = null,

    @Column(name = "crop_type", length = 50)
    val cropType: String? = null,

    @Column(length = 50)
    val timezone: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con Tenant.
     * Un invernadero pertenece a un tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    var tenant: Tenant? = null

    /**
     * Relación con sensores (lazy loading).
     * Un invernadero puede tener N sensores.
     */
    @OneToMany(mappedBy = "greenhouse", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @OrderBy("sensor_type ASC, device_id ASC")
    var sensors: MutableList<Sensor> = mutableListOf()

    /**
     * Relación con actuadores (lazy loading).
     * Un invernadero puede tener N actuadores.
     */
    @OneToMany(mappedBy = "greenhouse", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @OrderBy("actuator_type ASC, device_id ASC")
    var actuators: MutableList<Actuator> = mutableListOf()

    override fun toString(): String {
        return "Greenhouse(id=$id, name='$name', greenhouseCode=$greenhouseCode, mqttTopic=$mqttTopic, tenantId=$tenantId, isActive=$isActive)"
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
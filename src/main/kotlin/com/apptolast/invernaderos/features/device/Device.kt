package com.apptolast.invernaderos.features.device

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Categoría del dispositivo: SENSOR o ACTUATOR
 */
enum class DeviceCategory {
    SENSOR,
    ACTUATOR
}

/**
 * Entity unificada para dispositivos IoT (sensores + actuadores).
 * Reemplaza las tablas sensors y actuators con una estructura simplificada
 * usando CHECK constraints en lugar de FK a tablas de catálogo.
 *
 * @property id UUID único del dispositivo
 * @property tenantId UUID del tenant propietario
 * @property greenhouseId UUID del invernadero
 * @property code Código único del dispositivo (TEMP_01, FAN_02)
 * @property name Nombre descriptivo del dispositivo
 * @property hardwareId ID del hardware físico
 * @property category SENSOR o ACTUATOR
 * @property type Tipo de dispositivo (TEMPERATURE, VENTILATOR, etc.)
 * @property unit Unidad de medida (°C, %, ppm, etc.)
 * @property sector Código del sector (referencia al JSONB en greenhouses)
 * @property mqttTopic Topic MQTT para recibir datos
 * @property mqttCommandTopic Topic MQTT para enviar comandos (solo actuadores)
 * @property mqttFieldName Campo en payload JSON
 * @property lastValue Último valor registrado
 * @property state Estado actual (ON, OFF, AUTO, etc.) - solo actuadores
 * @property lastSeen Última vez que se vio el dispositivo
 * @property minThreshold Umbral mínimo para alertas (solo sensores)
 * @property maxThreshold Umbral máximo para alertas (solo sensores)
 * @property config Configuración adicional en JSONB
 * @property isActive Si está activo
 * @property createdAt Fecha de creación
 * @property updatedAt Fecha de actualización
 */
@NamedEntityGraph(
    name = "Device.context",
    attributeNodes = [NamedAttributeNode("greenhouse"), NamedAttributeNode("tenant")]
)
@Entity
@Table(
    name = "devices",
    schema = "metadata",
    indexes = [
        Index(name = "idx_devices_new_tenant", columnList = "tenant_id"),
        Index(name = "idx_devices_new_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_devices_new_category", columnList = "category"),
        Index(name = "idx_devices_new_type", columnList = "type"),
        Index(name = "idx_devices_new_active", columnList = "is_active"),
        Index(name = "idx_devices_new_last_seen", columnList = "last_seen"),
        Index(name = "idx_devices_new_sector", columnList = "greenhouse_id, sector")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_device_code", columnNames = ["greenhouse_id", "code"])
    ]
)
data class Device(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "code", length = 50, nullable = false)
    val code: String,

    @Column(name = "name", length = 100)
    val name: String? = null,

    @Column(name = "hardware_id", length = 50)
    val hardwareId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    val category: DeviceCategory,

    @Column(name = "type", length = 30, nullable = false)
    val type: String,

    @Column(name = "unit", length = 15)
    val unit: String? = null,

    @Column(name = "sector", length = 50)
    val sector: String? = null,

    @Column(name = "mqtt_topic", length = 150)
    val mqttTopic: String? = null,

    @Column(name = "mqtt_command_topic", length = 150)
    val mqttCommandTopic: String? = null,

    @Column(name = "mqtt_field_name", length = 100)
    val mqttFieldName: String? = null,

    @Column(name = "last_value")
    val lastValue: Double? = null,

    @Column(name = "state", length = 20)
    val state: String? = null,

    @Column(name = "last_seen")
    val lastSeen: Instant? = null,

    @Column(name = "min_threshold", precision = 10, scale = 2)
    val minThreshold: BigDecimal? = null,

    @Column(name = "max_threshold", precision = 10, scale = 2)
    val maxThreshold: BigDecimal? = null,

    @Column(name = "config", columnDefinition = "jsonb")
    val config: String? = "{}",

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

    val isSensor: Boolean get() = category == DeviceCategory.SENSOR
    val isActuator: Boolean get() = category == DeviceCategory.ACTUATOR

    override fun toString(): String {
        return "Device(id=$id, code='$code', category=$category, type='$type', greenhouseId=$greenhouseId, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Device) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

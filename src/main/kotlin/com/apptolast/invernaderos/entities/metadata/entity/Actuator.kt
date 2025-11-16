package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa un Actuador (dispositivo de control) en un invernadero.
 * Los actuadores controlan elementos físicos: ventiladores, riego, calefacción, etc.
 *
 * @property id UUID único del actuador
 * @property tenantId UUID del tenant propietario
 * @property greenhouseId UUID del invernadero al que pertenece
 * @property actuatorCode Código corto único del actuador (ej: FAN01, RIEGO02)
 * @property deviceId ID del dispositivo físico/hardware
 * @property actuatorType Tipo de actuador (VENTILADOR, RIEGO, CALEFACCION, etc.)
 * @property currentState Estado actual (ON, OFF, AUTO, MANUAL, ERROR, UNKNOWN)
 * @property currentValue Valor actual (ej: velocidad 0-100%, caudal L/h)
 * @property unit Unidad de medida del valor (%, L/h, RPM, etc.)
 * @property mqttCommandTopic Topic MQTT para enviar comandos al actuador
 * @property mqttStatusTopic Topic MQTT donde el actuador publica su estado
 * @property locationInGreenhouse Ubicación física dentro del invernadero
 * @property isActive Si el actuador está activo
 * @property lastCommandAt Timestamp del último comando enviado
 * @property lastStatusUpdate Timestamp de la última actualización de estado
 * @property createdAt Fecha de creación
 * @property updatedAt Fecha de última actualización
 */
@Entity
@Table(
    name = "actuators",
    schema = "metadata",
    indexes = [
        Index(name = "idx_actuators_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_actuators_tenant", columnList = "tenant_id"),
        Index(name = "idx_actuators_code", columnList = "actuator_code"),
        Index(name = "idx_actuators_device_id", columnList = "device_id"),
        Index(name = "idx_actuators_type", columnList = "actuator_type"),
        Index(name = "idx_actuators_greenhouse_active", columnList = "greenhouse_id, is_active"),
        Index(name = "idx_actuators_state", columnList = "current_state")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_actuator_code_per_greenhouse", columnNames = ["greenhouse_id", "actuator_code"]),
        UniqueConstraint(name = "uq_actuator_device_id_per_greenhouse", columnNames = ["greenhouse_id", "device_id"])
    ]
)
data class Actuator(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    /**
     * Código corto único del actuador dentro del greenhouse.
     * Ejemplo: "FAN01", "RIEGO02", "CALEF01"
     */
    @Column(name = "actuator_code", nullable = false, length = 50)
    val actuatorCode: String,

    @Column(name = "device_id", nullable = false, length = 50)
    val deviceId: String,

    /**
     * Tipo de actuador.
     * Valores posibles: VENTILADOR, FAN, RIEGO, IRRIGATION, CALEFACCION, HEATING,
     * ENFRIAMIENTO, COOLING, ILUMINACION, LIGHTING, CORTINA, CURTAIN,
     * VALVULA, VALVE, MOTOR, EXTRACTOR, OTHER
     */
    @Column(name = "actuator_type", nullable = false, length = 50)
    val actuatorType: String,

    /**
     * ID del tipo de actuador normalizado (SMALLINT).
     * References: metadata.actuator_types.id
     * Agregado en V13 para normalización
     */
    @Column(name = "actuator_type_id", columnDefinition = "SMALLINT")
    val actuatorTypeId: Short? = null,

    /**
     * Estado actual del actuador.
     * Valores posibles: ON, OFF, AUTO, MANUAL, ERROR, UNKNOWN
     */
    @Column(name = "current_state", length = 20)
    val currentState: String? = "UNKNOWN",

    /**
     * ID del estado normalizado (SMALLINT).
     * References: metadata.actuator_states.id
     * Agregado en V13 para normalización
     */
    @Column(name = "state_id", columnDefinition = "SMALLINT")
    val stateId: Short? = null,

    /**
     * Valor actual del actuador (ej: velocidad 0-100%, caudal L/h).
     */
    @Column(name = "current_value")
    val currentValue: Double? = null,

    /**
     * Unidad de medida del valor.
     * Ejemplos: "%", "L/h", "RPM", "W", "kW"
     */
    @Column(length = 20)
    val unit: String? = null,

    /**
     * ID de la unidad normalizada (SMALLINT).
     * References: metadata.units.id
     * Agregado en V13 para normalización
     */
    @Column(name = "unit_id", columnDefinition = "SMALLINT")
    val unitId: Short? = null,

    /**
     * Topic MQTT para enviar comandos al actuador.
     * Ejemplo: "GREENHOUSE/empresa001/actuator/FAN01/command"
     */
    @Column(name = "mqtt_command_topic", length = 100)
    val mqttCommandTopic: String? = null,

    /**
     * Topic MQTT donde el actuador publica su estado.
     * Ejemplo: "GREENHOUSE/empresa001/actuator/FAN01/status"
     */
    @Column(name = "mqtt_status_topic", length = 100)
    val mqttStatusTopic: String? = null,

    @Column(name = "location_in_greenhouse", length = 100)
    val locationInGreenhouse: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    /**
     * Timestamp del último comando enviado al actuador.
     */
    @Column(name = "last_command_at")
    val lastCommandAt: Instant? = null,

    /**
     * Timestamp de la última actualización de estado recibida del actuador.
     */
    @Column(name = "last_status_update")
    val lastStatusUpdate: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con Greenhouse.
     * Un actuador pertenece a un greenhouse.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenhouse_id", referencedColumnName = "id", insertable = false, updatable = false)
    var greenhouse: Greenhouse? = null

    /**
     * Relación ManyToOne con Tenant.
     * Un actuador pertenece a un tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    var tenant: Tenant? = null

    override fun toString(): String {
        return "Actuator(id=$id, actuatorCode='$actuatorCode', deviceId='$deviceId', actuatorType='$actuatorType', currentState=$currentState, greenhouseId=$greenhouseId, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Actuator) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        /**
         * Estados válidos para actuadores.
         */
        object State {
            const val ON = "ON"
            const val OFF = "OFF"
            const val AUTO = "AUTO"
            const val MANUAL = "MANUAL"
            const val ERROR = "ERROR"
            const val UNKNOWN = "UNKNOWN"
        }

        /**
         * Tipos de actuadores comunes.
         */
        object Type {
            const val VENTILADOR = "VENTILADOR"
            const val FAN = "FAN"
            const val RIEGO = "RIEGO"
            const val IRRIGATION = "IRRIGATION"
            const val CALEFACCION = "CALEFACCION"
            const val HEATING = "HEATING"
            const val ENFRIAMIENTO = "ENFRIAMIENTO"
            const val COOLING = "COOLING"
            const val ILUMINACION = "ILUMINACION"
            const val LIGHTING = "LIGHTING"
            const val CORTINA = "CORTINA"
            const val CURTAIN = "CURTAIN"
            const val VALVULA = "VALVULA"
            const val VALVE = "VALVE"
            const val MOTOR = "MOTOR"
            const val EXTRACTOR = "EXTRACTOR"
            const val OTHER = "OTHER"
        }
    }
}

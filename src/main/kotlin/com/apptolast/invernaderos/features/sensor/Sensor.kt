package com.apptolast.invernaderos.features.sensor

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa un Sensor en un invernadero. Los sensores recopilan datos (temperatura,
 * humedad, etc.) y envían vía MQTT.
 *
 * @property id UUID único del sensor
 * @property greenhouseId UUID del invernadero al que pertenece
 * @property tenantId UUID del tenant (denormalizado para queries optimizados)
 * @property sensorCode Código corto del sensor (ej: TEMP01, HUM02)
 * @property deviceId ID del dispositivo físico/hardware
 * @property sensorType Tipo de sensor (TEMPERATURE, HUMIDITY, PRESSURE, etc.)
 * @property mqttFieldName Nombre del campo en payload JSON MQTT (ej: empresaID_sensorID,
 * TEMPERATURA_INVERNADERO_01)
 * @property dataFormat Formato de datos: NUMERIC, STRING, JSON, BOOLEAN
 * @property unit Unidad de medida (°C, %, hPa, etc.)
 * @property minThreshold Umbral mínimo para alertas
 * @property maxThreshold Umbral máximo para alertas
 * @property locationInGreenhouse Ubicación física dentro del invernadero
 * @property calibrationData Datos de calibración en JSONB
 * @property isActive Si el sensor está activo
 * @property lastSeen Última vez que se recibieron datos del sensor
 * @property createdAt Fecha de creación
 * @property updatedAt Fecha de última actualización
 */
@NamedEntityGraph(
        name = "Sensor.context",
        attributeNodes = [NamedAttributeNode("greenhouse"), NamedAttributeNode("tenant")]
)
@Entity
@Table(
        name = "sensors",
        schema = "metadata",
        indexes =
                [
                        Index(name = "idx_sensors_greenhouse", columnList = "greenhouse_id"),
                        Index(name = "idx_sensors_tenant", columnList = "tenant_id"),
                        Index(name = "idx_sensors_code", columnList = "sensor_code"),
                        Index(name = "idx_sensors_mqtt_field", columnList = "mqtt_field_name"),
                        Index(name = "idx_sensors_device_id", columnList = "device_id"),
                        Index(
                                name = "idx_sensors_greenhouse_active",
                                columnList = "greenhouse_id, is_active"
                        ),
                        Index(
                                name = "idx_sensors_type_active",
                                columnList = "sensor_type, is_active"
                        )],
        uniqueConstraints =
                [
                        UniqueConstraint(
                                name = "uq_sensor_code_per_greenhouse",
                                columnNames = ["greenhouse_id", "sensor_code"]
                        ),
                        UniqueConstraint(
                                name = "uq_mqtt_field_per_greenhouse",
                                columnNames = ["greenhouse_id", "mqtt_field_name"]
                        )]
)
data class Sensor(
        @Id @GeneratedValue(strategy = GenerationType.AUTO) val id: UUID? = null,
        @Column(name = "greenhouse_id", nullable = false) val greenhouseId: UUID,

        /**
         * Tenant ID denormalizado para queries directos sin JOIN. Mejora performance en queries
         * multi-tenant.
         */
        @Column(name = "tenant_id") val tenantId: UUID? = null,

        /**
         * Código corto único del sensor dentro del greenhouse. Ejemplo: "TEMP01", "HUM02", "PRES01"
         * Usado para identificación en mensajes MQTT.
         */
        @Column(name = "sensor_code", length = 50) val sensorCode: String? = null,
        @Column(name = "device_id", nullable = false, length = 50) val deviceId: String,
        @Column(name = "sensor_type", nullable = false, length = 50) val sensorType: String,

        /**
         * ID del tipo de sensor normalizado (SMALLINT). References: metadata.sensor_types.id
         * Agregado en V13 para normalización (ahorra espacio: VARCHAR 50 → SMALLINT 2 bytes)
         */
        @Column(name = "sensor_type_id", columnDefinition = "SMALLINT")
        val sensorTypeId: Short? = null,

        /**
         * Nombre del campo en payload JSON MQTT. Ejemplos:
         * - "empresaID_sensorID": "001_TEMP01"
         * - "TEMPERATURA INVERNADERO 01" (formato legacy con espacios)
         * - "INVERNADERO_01_SECTOR_01" (formato con underscores)
         */
        @Column(name = "mqtt_field_name", length = 100) val mqttFieldName: String? = null,

        /**
         * Formato de datos del sensor. Valores posibles: NUMERIC, STRING, JSON, BOOLEAN Default:
         * NUMERIC
         */
        @Column(name = "data_format", length = 20) val dataFormat: String? = "NUMERIC",
        @Column(length = 20) val unit: String? = null,

        /**
         * ID de la unidad normalizada (SMALLINT). References: metadata.units.id Agregado en V13
         * para normalización
         */
        @Column(name = "unit_id", columnDefinition = "SMALLINT") val unitId: Short? = null,
        @Column(name = "min_threshold", precision = 10, scale = 2)
        val minThreshold: BigDecimal? = null,
        @Column(name = "max_threshold", precision = 10, scale = 2)
        val maxThreshold: BigDecimal? = null,
        @Column(name = "location_in_greenhouse", length = 100)
        val locationInGreenhouse: String? = null,
        @Column(name = "calibration_data", columnDefinition = "jsonb")
        val calibrationData: String? = null,
        @Column(name = "is_active", nullable = false) val isActive: Boolean = true,
        @Column(name = "last_seen") val lastSeen: Instant? = null,
        @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
        @Column(name = "updated_at", nullable = false) val updatedAt: Instant = Instant.now()
) {
    /** Relación ManyToOne con Greenhouse. Un sensor pertenece a un greenhouse. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "greenhouse_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    var greenhouse: Greenhouse? = null

    /** Relación ManyToOne con Tenant (denormalizado). Un sensor pertenece a un tenant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tenant_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    var tenant: Tenant? = null

    override fun toString(): String {
        return "Sensor(id=$id, sensorCode=$sensorCode, deviceId='$deviceId', sensorType='$sensorType', mqttFieldName=$mqttFieldName, greenhouseId=$greenhouseId, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sensor) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}

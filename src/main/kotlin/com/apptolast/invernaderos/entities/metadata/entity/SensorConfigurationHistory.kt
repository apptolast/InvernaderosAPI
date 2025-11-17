package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Entidad READONLY para historial de cambios de configuración de sensores.
 * Registra old/new values cuando se modifican thresholds, calibration, etc.
 *
 * @property id ID autoincremental
 * @property sensorId UUID del sensor configurado
 * @property oldSensorTypeId Tipo de sensor anterior
 * @property oldUnitId Unidad anterior
 * @property oldMinThreshold Umbral mínimo anterior
 * @property oldMaxThreshold Umbral máximo anterior
 * @property oldCalibrationData Datos de calibración anteriores (JSONB)
 * @property oldMqttFieldName Campo MQTT anterior
 * @property newSensorTypeId Nuevo tipo de sensor
 * @property newUnitId Nueva unidad
 * @property newMinThreshold Nuevo umbral mínimo
 * @property newMaxThreshold Nuevo umbral máximo
 * @property newCalibrationData Nuevos datos de calibración (JSONB)
 * @property newMqttFieldName Nuevo campo MQTT
 * @property changedBy UUID del usuario que hizo el cambio
 * @property changedAt Timestamp del cambio
 * @property changeReason Razón del cambio
 * @property createdAt Timestamp de creación del registro
 */
@Entity
@Table(name = "sensor_configuration_history", schema = "metadata")
@Immutable
data class SensorConfigurationHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "sensor_id", nullable = false)
    val sensorId: UUID,

    @Column(name = "old_sensor_type_id")
    val oldSensorTypeId: Short? = null,

    @Column(name = "old_unit_id")
    val oldUnitId: Short? = null,

    @Column(name = "old_min_threshold", precision = 10, scale = 2)
    val oldMinThreshold: BigDecimal? = null,

    @Column(name = "old_max_threshold", precision = 10, scale = 2)
    val oldMaxThreshold: BigDecimal? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_calibration_data", columnDefinition = "jsonb")
    val oldCalibrationData: String? = null,

    @Column(name = "old_mqtt_field_name", length = 100)
    val oldMqttFieldName: String? = null,

    @Column(name = "new_sensor_type_id")
    val newSensorTypeId: Short? = null,

    @Column(name = "new_unit_id")
    val newUnitId: Short? = null,

    @Column(name = "new_min_threshold", precision = 10, scale = 2)
    val newMinThreshold: BigDecimal? = null,

    @Column(name = "new_max_threshold", precision = 10, scale = 2)
    val newMaxThreshold: BigDecimal? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_calibration_data", columnDefinition = "jsonb")
    val newCalibrationData: String? = null,

    @Column(name = "new_mqtt_field_name", length = 100)
    val newMqttFieldName: String? = null,

    @Column(name = "changed_by")
    val changedBy: UUID? = null,

    @Column(name = "changed_at", nullable = false)
    val changedAt: Instant,

    @Column(name = "change_reason", columnDefinition = "TEXT")
    val changeReason: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorConfigurationHistory) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String {
        return "SensorConfigurationHistory(id=$id, sensorId=$sensorId, changedAt=$changedAt)"
    }
}

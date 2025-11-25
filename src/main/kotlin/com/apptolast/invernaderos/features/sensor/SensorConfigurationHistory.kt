package com.apptolast.invernaderos.features.sensor

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import com.apptolast.invernaderos.features.user.User

/**
 * Entity para registrar cambios en la configuración de sensores.
 * Permite rastrear el historial completo de modificaciones en sensores.
 *
 * @property id ID único del registro (BIGSERIAL)
 * @property sensorId UUID del sensor modificado
 * @property changedBy UUID del usuario que realizó el cambio
 * @property oldSensorTypeId ID del tipo de sensor anterior
 * @property oldUnitId ID de la unidad anterior
 * @property oldMinThreshold Umbral mínimo anterior
 * @property oldMaxThreshold Umbral máximo anterior
 * @property oldCalibrationData Datos de calibración anteriores (JSONB)
 * @property oldMqttFieldName Nombre del campo MQTT anterior
 * @property newSensorTypeId Nuevo ID del tipo de sensor
 * @property newUnitId Nuevo ID de la unidad
 * @property newMinThreshold Nuevo umbral mínimo
 * @property newMaxThreshold Nuevo umbral máximo
 * @property newCalibrationData Nuevos datos de calibración (JSONB)
 * @property newMqttFieldName Nuevo nombre del campo MQTT
 * @property changedAt Timestamp del cambio
 * @property changeReason Razón del cambio
 * @property changeType Tipo de cambio: CALIBRATION, THRESHOLD_ADJUSTMENT, TYPE_CHANGE, etc.
 * @property createdAt Fecha de creación del registro
 */
@Entity
@Table(
    name = "sensor_configuration_history",
    schema = "metadata",
    indexes = [
        Index(name = "idx_sensor_config_history_sensor", columnList = "sensor_id"),
        Index(name = "idx_sensor_config_history_changed_by", columnList = "changed_by"),
        Index(name = "idx_sensor_config_history_changed_at", columnList = "changed_at"),
        Index(name = "idx_sensor_config_history_change_type", columnList = "change_type")
    ]
)
data class SensorConfigurationHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "sensor_id", nullable = false)
    val sensorId: UUID,

    @Column(name = "changed_by")
    val changedBy: UUID? = null,

    // Valores antiguos
    @Column(name = "old_sensor_type_id", columnDefinition = "SMALLINT")
    val oldSensorTypeId: Short? = null,

    @Column(name = "old_unit_id", columnDefinition = "SMALLINT")
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

    // Valores nuevos
    @Column(name = "new_sensor_type_id", columnDefinition = "SMALLINT")
    val newSensorTypeId: Short? = null,

    @Column(name = "new_unit_id", columnDefinition = "SMALLINT")
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

    @Column(name = "changed_at", nullable = false)
    val changedAt: Instant = Instant.now(),

    /**
     * Razón del cambio.
     * Ejemplo: "Recalibración del sensor después de mantenimiento"
     */
    @Column(name = "change_reason", columnDefinition = "TEXT")
    val changeReason: String? = null,

    /**
     * Tipo de cambio realizado.
     * Ejemplos: CALIBRATION, THRESHOLD_ADJUSTMENT, TYPE_CHANGE, MQTT_CONFIG_CHANGE
     */
    @Column(name = "change_type", length = 50)
    val changeType: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con Sensor.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", referencedColumnName = "id", insertable = false, updatable = false)
    var sensor: Sensor? = null

    /**
     * Relación ManyToOne con User (usuario que realizó el cambio).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", referencedColumnName = "id", insertable = false, updatable = false)
    var user: User? = null

    override fun toString(): String {
        return "SensorConfigurationHistory(id=$id, sensorId=$sensorId, changeType=$changeType, changedAt=$changedAt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorConfigurationHistory) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object ChangeType {
            const val CALIBRATION = "CALIBRATION"
            const val THRESHOLD_ADJUSTMENT = "THRESHOLD_ADJUSTMENT"
            const val TYPE_CHANGE = "TYPE_CHANGE"
            const val MQTT_CONFIG_CHANGE = "MQTT_CONFIG_CHANGE"
            const val UNIT_CHANGE = "UNIT_CHANGE"
        }
    }
}

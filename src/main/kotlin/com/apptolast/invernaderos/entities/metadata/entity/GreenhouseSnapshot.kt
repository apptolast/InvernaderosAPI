package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Entidad para snapshots completos del estado de un invernadero.
 * Permite guardar configuraciones, estados de sensores/actuadores, y métricas en un momento específico.
 *
 * ⚠️ A diferencia de las otras entidades de auditoría, esta SÍ es modificable (puede ser usada para caching).
 *
 * @property id ID autoincremental
 * @property greenhouseId UUID del invernadero
 * @property tenantId UUID del tenant
 * @property snapshotAt Timestamp del snapshot
 * @property snapshotType Tipo de snapshot: SCHEDULED, MANUAL, BEFORE_CHANGE, BACKUP
 * @property greenhouseConfig Configuración completa del invernadero (JSONB)
 * @property sensorsConfig Configuración de sensores (JSONB)
 * @property actuatorsConfig Configuración de actuadores (JSONB)
 * @property activeAlerts Alertas activas en el momento del snapshot (JSONB)
 * @property automationRules Reglas de automatización (JSONB)
 * @property avgTemperature Temperatura promedio en el periodo del snapshot
 * @property avgHumidity Humedad promedio
 * @property totalReadings Total de lecturas de sensores
 * @property totalAlerts Total de alertas
 * @property totalCommands Total de comandos de actuadores
 * @property notes Notas sobre el snapshot
 * @property createdAt Timestamp de creación del registro
 * @property updatedAt Timestamp de última actualización (permite updates)
 */
@Entity
@Table(
    name = "greenhouse_snapshot",
    schema = "metadata",
    indexes = [
        Index(name = "idx_snapshot_greenhouse_time", columnList = "greenhouse_id, snapshot_at"),
        Index(name = "idx_snapshot_tenant_time", columnList = "tenant_id, snapshot_at"),
        Index(name = "idx_snapshot_type", columnList = "snapshot_type")
    ]
)
data class GreenhouseSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "snapshot_at", nullable = false)
    val snapshotAt: Instant,

    @Column(name = "snapshot_type", length = 20, nullable = false)
    val snapshotType: String = "SCHEDULED",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "greenhouse_config", columnDefinition = "jsonb", nullable = false)
    val greenhouseConfig: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sensors_config", columnDefinition = "jsonb", nullable = false)
    val sensorsConfig: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actuators_config", columnDefinition = "jsonb", nullable = false)
    val actuatorsConfig: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "active_alerts", columnDefinition = "jsonb")
    val activeAlerts: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "automation_rules", columnDefinition = "jsonb")
    val automationRules: String? = null,

    @Column(name = "avg_temperature", precision = 5, scale = 2)
    val avgTemperature: BigDecimal? = null,

    @Column(name = "avg_humidity", precision = 5, scale = 2)
    val avgHumidity: BigDecimal? = null,

    @Column(name = "total_readings")
    val totalReadings: Int? = null,

    @Column(name = "total_alerts")
    val totalAlerts: Int? = null,

    @Column(name = "total_commands")
    val totalCommands: Int? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GreenhouseSnapshot) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String {
        return "GreenhouseSnapshot(id=$id, greenhouseId=$greenhouseId, snapshotAt=$snapshotAt, snapshotType='$snapshotType')"
    }
}

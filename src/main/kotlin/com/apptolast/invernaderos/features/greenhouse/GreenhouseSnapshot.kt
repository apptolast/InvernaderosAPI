package com.apptolast.invernaderos.features.greenhouse

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import com.apptolast.invernaderos.features.tenant.Tenant
import com.apptolast.invernaderos.features.user.User

/**
 * Entity para almacenar snapshots (capturas de estado) de invernaderos.
 * Permite mantener un historial de configuraciones y estados del invernadero en momentos específicos.
 *
 * @property id ID único del snapshot (BIGSERIAL)
 * @property greenhouseId UUID del invernadero
 * @property tenantId UUID del tenant
 * @property createdBy UUID del usuario que creó el snapshot
 * @property snapshotAt Timestamp del snapshot
 * @property snapshotType Tipo: SCHEDULED, MANUAL, BEFORE_CHANGE, BACKUP
 * @property greenhouseConfig Configuración del invernadero en formato JSONB
 * @property sensorsConfig Configuración de sensores en formato JSONB
 * @property actuatorsConfig Configuración de actuadores en formato JSONB
 * @property activeAlerts Alertas activas en el momento del snapshot (JSONB)
 * @property automationRules Reglas de automatización activas (JSONB)
 * @property avgTemperature Temperatura promedio en el momento del snapshot
 * @property avgHumidity Humedad promedio en el momento del snapshot
 * @property totalReadings Total de lecturas de sensores en las últimas 24h
 * @property totalAlerts Total de alertas generadas en las últimas 24h
 * @property totalCommands Total de comandos enviados a actuadores en las últimas 24h
 * @property notes Notas adicionales sobre el snapshot
 * @property createdAt Fecha de creación del registro
 */
@Entity
@Table(
    name = "greenhouse_snapshot",
    schema = "metadata",
    indexes = [
        Index(name = "idx_greenhouse_snapshot_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_greenhouse_snapshot_tenant", columnList = "tenant_id"),
        Index(name = "idx_greenhouse_snapshot_created_by", columnList = "created_by"),
        Index(name = "idx_greenhouse_snapshot_snapshot_at", columnList = "snapshot_at"),
        Index(name = "idx_greenhouse_snapshot_type", columnList = "snapshot_type")
    ]
)
data class GreenhouseSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "created_by")
    val createdBy: UUID? = null,

    @Column(name = "snapshot_at", nullable = false)
    val snapshotAt: Instant = Instant.now(),

    /**
     * Tipo de snapshot:
     * - SCHEDULED: Snapshot programado (ej: diario, semanal)
     * - MANUAL: Snapshot creado manualmente por usuario
     * - BEFORE_CHANGE: Snapshot antes de un cambio significativo
     * - BACKUP: Snapshot de respaldo
     */
    @Column(name = "snapshot_type", nullable = false, length = 20)
    val snapshotType: String = SnapshotType.SCHEDULED,

    /**
     * Configuración completa del invernadero en formato JSONB.
     * Incluye: nombre, ubicación, área, tipo de cultivo, timezone, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "greenhouse_config", nullable = false, columnDefinition = "jsonb")
    val greenhouseConfig: String,

    /**
     * Configuración de todos los sensores del invernadero en formato JSONB.
     * Array de objetos con: sensor_id, tipo, umbrales, ubicación, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sensors_config", nullable = false, columnDefinition = "jsonb")
    val sensorsConfig: String,

    /**
     * Configuración de todos los actuadores del invernadero en formato JSONB.
     * Array de objetos con: actuator_id, tipo, estado, configuración, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actuators_config", nullable = false, columnDefinition = "jsonb")
    val actuatorsConfig: String,

    /**
     * Alertas activas en el momento del snapshot (JSONB).
     * Array de alertas no resueltas.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "active_alerts", columnDefinition = "jsonb")
    val activeAlerts: String? = null,

    /**
     * Reglas de automatización activas (JSONB).
     * Array de reglas configuradas.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "automation_rules", columnDefinition = "jsonb")
    val automationRules: String? = null,

    /**
     * Temperatura promedio del invernadero en el momento del snapshot.
     */
    @Column(name = "avg_temperature", precision = 5, scale = 2)
    val avgTemperature: BigDecimal? = null,

    /**
     * Humedad promedio del invernadero en el momento del snapshot.
     */
    @Column(name = "avg_humidity", precision = 5, scale = 2)
    val avgHumidity: BigDecimal? = null,

    /**
     * Total de lecturas de sensores en las últimas 24 horas.
     */
    @Column(name = "total_readings")
    val totalReadings: Int? = null,

    /**
     * Total de alertas generadas en las últimas 24 horas.
     */
    @Column(name = "total_alerts")
    val totalAlerts: Int? = null,

    /**
     * Total de comandos enviados a actuadores en las últimas 24 horas.
     */
    @Column(name = "total_commands")
    val totalCommands: Int? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con Greenhouse.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenhouse_id", referencedColumnName = "id", insertable = false, updatable = false)
    var greenhouse: Greenhouse? = null

    /**
     * Relación ManyToOne con Tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    var tenant: Tenant? = null

    /**
     * Relación ManyToOne con User (usuario que creó el snapshot).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
    var user: User? = null

    override fun toString(): String {
        return "GreenhouseSnapshot(id=$id, greenhouseId=$greenhouseId, snapshotType='$snapshotType', snapshotAt=$snapshotAt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GreenhouseSnapshot) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object SnapshotType {
            const val SCHEDULED = "SCHEDULED"
            const val MANUAL = "MANUAL"
            const val BEFORE_CHANGE = "BEFORE_CHANGE"
            const val BACKUP = "BACKUP"
        }
    }
}

package com.apptolast.invernaderos.features.audit

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID
import com.apptolast.invernaderos.features.user.User
import com.apptolast.invernaderos.features.sensor.Sensor
import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant

/**
 * Entity para registrar problemas de calidad de datos detectados en el sistema.
 * Ayuda a identificar datos faltantes, duplicados, fuera de rango, inconsistentes, etc.
 *
 * @property id ID único del registro (BIGSERIAL)
 * @property dataSource Fuente de datos donde se detectó el problema
 * @property qualityIssueType Tipo de problema: MISSING_DATA, OUT_OF_RANGE, DUPLICATE, INCONSISTENT, etc.
 * @property severity Severidad: LOW, MEDIUM, HIGH, CRITICAL
 * @property greenhouseId UUID del invernadero afectado
 * @property sensorId UUID del sensor afectado
 * @property tenantId UUID del tenant afectado
 * @property resolvedBy UUID del usuario que resolvió el problema
 * @property detectedAt Timestamp de detección del problema
 * @property timeRangeStart Inicio del rango temporal afectado
 * @property timeRangeEnd Fin del rango temporal afectado
 * @property affectedRecordsCount Cantidad de registros afectados
 * @property description Descripción del problema
 * @property sampleData Datos de ejemplo en formato JSONB
 * @property status Estado: OPEN, INVESTIGATING, RESOLVED, IGNORED
 * @property resolvedAt Timestamp de resolución
 * @property resolutionNotes Notas sobre la resolución
 * @property createdAt Fecha de creación del registro
 */
@Entity
@Table(
    name = "data_quality_log",
    schema = "metadata",
    indexes = [
        Index(name = "idx_dql_data_source", columnList = "data_source"),
        Index(name = "idx_dql_issue_type", columnList = "quality_issue_type"),
        Index(name = "idx_dql_severity", columnList = "severity"),
        Index(name = "idx_dql_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_dql_sensor", columnList = "sensor_id"),
        Index(name = "idx_dql_tenant", columnList = "tenant_id"),
        Index(name = "idx_dql_status", columnList = "status"),
        Index(name = "idx_dql_detected_at", columnList = "detected_at")
    ]
)
data class DataQualityLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * Fuente de datos donde se detectó el problema.
     * Ejemplos: "MQTT_BROKER", "SENSOR_READINGS", "TIMESCALEDB", "API"
     */
    @Column(name = "data_source", nullable = false, length = 50)
    val dataSource: String,

    /**
     * Tipo de problema de calidad de datos.
     * Ejemplos: MISSING_DATA, OUT_OF_RANGE, DUPLICATE, INCONSISTENT, STALE_DATA, ANOMALY
     */
    @Column(name = "quality_issue_type", nullable = false, length = 50)
    val qualityIssueType: String,

    /**
     * Severidad del problema: LOW, MEDIUM, HIGH, CRITICAL
     */
    @Column(length = 20)
    val severity: String? = null,

    @Column(name = "greenhouse_id")
    val greenhouseId: UUID? = null,

    @Column(name = "sensor_id")
    val sensorId: UUID? = null,

    @Column(name = "tenant_id")
    val tenantId: UUID? = null,

    @Column(name = "resolved_by")
    val resolvedBy: UUID? = null,

    @Column(name = "detected_at", nullable = false)
    val detectedAt: Instant = Instant.now(),

    /**
     * Inicio del rango temporal afectado por el problema.
     */
    @Column(name = "time_range_start")
    val timeRangeStart: Instant? = null,

    /**
     * Fin del rango temporal afectado por el problema.
     */
    @Column(name = "time_range_end")
    val timeRangeEnd: Instant? = null,

    /**
     * Cantidad de registros afectados por el problema.
     */
    @Column(name = "affected_records_count")
    val affectedRecordsCount: Int? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    /**
     * Datos de ejemplo del problema en formato JSONB.
     * Ejemplo: {"sensor_id": "TEMP01", "expected_range": [0, 50], "actual_value": 999}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sample_data", columnDefinition = "jsonb")
    val sampleData: String? = null,

    /**
     * Estado del problema:
     * - OPEN: Detectado pero no investigado
     * - INVESTIGATING: En investigación
     * - RESOLVED: Resuelto
     * - IGNORED: Ignorado (falso positivo o no crítico)
     */
    @Column(nullable = false, length = 20)
    val status: String = Status.OPEN,

    @Column(name = "resolved_at")
    val resolvedAt: Instant? = null,

    /**
     * Notas sobre cómo se resolvió el problema.
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    val resolutionNotes: String? = null,

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
     * Relación ManyToOne con Sensor.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", referencedColumnName = "id", insertable = false, updatable = false)
    var sensor: Sensor? = null

    /**
     * Relación ManyToOne con Tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    var tenant: Tenant? = null

    /**
     * Relación ManyToOne con User (usuario que resolvió el problema).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by", referencedColumnName = "id", insertable = false, updatable = false)
    var resolvedByUser: User? = null

    override fun toString(): String {
        return "DataQualityLog(id=$id, dataSource='$dataSource', qualityIssueType='$qualityIssueType', severity=$severity, status='$status', detectedAt=$detectedAt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataQualityLog) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object Status {
            const val OPEN = "OPEN"
            const val INVESTIGATING = "INVESTIGATING"
            const val RESOLVED = "RESOLVED"
            const val IGNORED = "IGNORED"
        }

        object Severity {
            const val LOW = "LOW"
            const val MEDIUM = "MEDIUM"
            const val HIGH = "HIGH"
            const val CRITICAL = "CRITICAL"
        }

        object IssueType {
            const val MISSING_DATA = "MISSING_DATA"
            const val OUT_OF_RANGE = "OUT_OF_RANGE"
            const val DUPLICATE = "DUPLICATE"
            const val INCONSISTENT = "INCONSISTENT"
            const val STALE_DATA = "STALE_DATA"
            const val ANOMALY = "ANOMALY"
        }
    }
}

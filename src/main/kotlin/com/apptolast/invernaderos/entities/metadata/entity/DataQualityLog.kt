package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Entidad READONLY para log de problemas de calidad de datos.
 * Registra datos fuera de rango, missing data, duplicados, spikes, drift, etc.
 *
 * @property id ID autoincremental
 * @property dataSource Fuente de datos: SENSOR_READINGS, MQTT, API, MANUAL_ENTRY
 * @property qualityIssueType Tipo de problema: OUT_OF_RANGE, MISSING_DATA, DUPLICATE, SPIKE, DRIFT
 * @property severity Severidad: LOW, MEDIUM, HIGH, CRITICAL
 * @property greenhouseId UUID del invernadero afectado
 * @property sensorId UUID del sensor afectado (si aplica)
 * @property tenantId UUID del tenant
 * @property detectedAt Timestamp de detección del problema
 * @property timeRangeStart Inicio del rango de tiempo afectado
 * @property timeRangeEnd Fin del rango de tiempo afectado
 * @property affectedRecords Número de registros afectados
 * @property issueDetails Detalles del problema en JSONB
 * @property expectedRange Rango esperado de valores
 * @property actualValues Valores reales encontrados
 * @property autoResolved Si el problema se resolvió automáticamente
 * @property resolutionAction Acción tomada para resolver
 * @property createdAt Timestamp de creación del registro
 */
@Entity
@Table(name = "data_quality_log", schema = "metadata")
@Immutable
data class DataQualityLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "data_source", nullable = false, length = 50)
    val dataSource: String,

    @Column(name = "quality_issue_type", nullable = false, length = 50)
    val qualityIssueType: String,

    @Column(name = "severity", length = 20)
    val severity: String? = null,

    @Column(name = "greenhouse_id")
    val greenhouseId: UUID? = null,

    @Column(name = "sensor_id")
    val sensorId: UUID? = null,

    @Column(name = "tenant_id")
    val tenantId: UUID? = null,

    @Column(name = "detected_at", nullable = false)
    val detectedAt: Instant,

    @Column(name = "time_range_start")
    val timeRangeStart: Instant? = null,

    @Column(name = "time_range_end")
    val timeRangeEnd: Instant? = null,

    @Column(name = "affected_records")
    val affectedRecords: Int? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issue_details", columnDefinition = "jsonb")
    val issueDetails: String? = null,

    @Column(name = "expected_range", length = 100)
    val expectedRange: String? = null,

    @Column(name = "actual_values", columnDefinition = "TEXT")
    val actualValues: String? = null,

    @Column(name = "auto_resolved", nullable = false)
    val autoResolved: Boolean = false,

    @Column(name = "resolution_action", columnDefinition = "TEXT")
    val resolutionAction: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataQualityLog) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String {
        return "DataQualityLog(id=$id, dataSource='$dataSource', qualityIssueType='$qualityIssueType', severity='$severity', detectedAt=$detectedAt)"
    }
}

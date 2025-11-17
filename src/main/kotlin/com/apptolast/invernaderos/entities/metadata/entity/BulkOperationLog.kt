package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Entidad READONLY para log de operaciones bulk/masivas.
 * Registra imports masivos, batch updates, calibraciones en lote, etc.
 *
 * @property id ID autoincremental
 * @property operationType Tipo de operación: SENSOR_IMPORT, ACTUATOR_UPDATE, CALIBRATION_BATCH
 * @property targetTable Tabla objetivo de la operación
 * @property tenantId UUID del tenant (si aplica)
 * @property startedAt Timestamp de inicio
 * @property completedAt Timestamp de finalización
 * @property status Estado: RUNNING, COMPLETED, FAILED, PARTIAL, CANCELLED
 * @property totalRecords Total de registros procesados
 * @property successfulRecords Registros exitosos
 * @property failedRecords Registros fallidos
 * @property skippedRecords Registros omitidos
 * @property errorSummary Resumen de errores en JSONB
 * @property errorDetails Detalles de errores
 * @property executedBy UUID del usuario que ejecutó la operación
 * @property durationSeconds Duración en segundos
 * @property createdAt Timestamp de creación del registro
 */
@Entity
@Table(name = "bulk_operation_log", schema = "metadata")
@Immutable
data class BulkOperationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "operation_type", nullable = false, length = 50)
    val operationType: String,

    @Column(name = "target_table", nullable = false, length = 100)
    val targetTable: String,

    @Column(name = "tenant_id")
    val tenantId: UUID? = null,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,

    @Column(name = "completed_at")
    val completedAt: Instant? = null,

    @Column(name = "status", length = 20, nullable = false)
    val status: String = "RUNNING",

    @Column(name = "total_records", nullable = false)
    val totalRecords: Int = 0,

    @Column(name = "successful_records", nullable = false)
    val successfulRecords: Int = 0,

    @Column(name = "failed_records", nullable = false)
    val failedRecords: Int = 0,

    @Column(name = "skipped_records", nullable = false)
    val skippedRecords: Int = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_summary", columnDefinition = "jsonb")
    val errorSummary: String? = null,

    @Column(name = "error_details", columnDefinition = "TEXT")
    val errorDetails: String? = null,

    @Column(name = "executed_by")
    val executedBy: UUID? = null,

    @Column(name = "duration_seconds", precision = 10, scale = 2)
    val durationSeconds: java.math.BigDecimal? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BulkOperationLog) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String {
        return "BulkOperationLog(id=$id, operationType='$operationType', status='$status', totalRecords=$totalRecords, successfulRecords=$successfulRecords)"
    }
}

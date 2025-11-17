package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Entity para registrar operaciones masivas (bulk operations) en el sistema.
 * Útil para auditar imports, exports, migraciones de datos, y operaciones batch.
 *
 * @property id ID único de la operación (BIGSERIAL)
 * @property operationType Tipo de operación: IMPORT, EXPORT, MIGRATION, BATCH_UPDATE, BATCH_DELETE
 * @property targetTable Tabla objetivo de la operación
 * @property tenantId UUID del tenant (para operaciones multi-tenant)
 * @property triggeredBy UUID del usuario que disparó la operación
 * @property startedAt Timestamp de inicio de la operación
 * @property completedAt Timestamp de finalización de la operación
 * @property status Estado: RUNNING, COMPLETED, FAILED, PARTIAL, CANCELLED
 * @property totalRecords Total de registros a procesar
 * @property successfulRecords Registros procesados exitosamente
 * @property failedRecords Registros que fallaron
 * @property skippedRecords Registros omitidos (ej: duplicados)
 * @property errorSummary Resumen de errores en formato JSONB
 * @property errorDetails Detalles de errores en formato texto
 * @property durationSeconds Duración de la operación en segundos
 * @property recordsPerSecond Throughput (registros por segundo)
 * @property sourceFile Archivo de origen (para imports/exports)
 * @property metadata Metadata adicional en formato JSONB
 * @property createdAt Fecha de creación del registro
 */
@Entity
@Table(
    name = "bulk_operation_log",
    schema = "metadata",
    indexes = [
        Index(name = "idx_bulk_op_log_operation_type", columnList = "operation_type"),
        Index(name = "idx_bulk_op_log_target_table", columnList = "target_table"),
        Index(name = "idx_bulk_op_log_tenant", columnList = "tenant_id"),
        Index(name = "idx_bulk_op_log_triggered_by", columnList = "triggered_by"),
        Index(name = "idx_bulk_op_log_status", columnList = "status"),
        Index(name = "idx_bulk_op_log_started_at", columnList = "started_at")
    ]
)
data class BulkOperationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "operation_type", nullable = false, length = 50)
    val operationType: String,

    @Column(name = "target_table", nullable = false, length = 100)
    val targetTable: String,

    @Column(name = "tenant_id")
    val tenantId: UUID? = null,

    @Column(name = "triggered_by")
    val triggeredBy: UUID? = null,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant = Instant.now(),

    @Column(name = "completed_at")
    val completedAt: Instant? = null,

    /**
     * Estado de la operación:
     * - RUNNING: En ejecución
     * - COMPLETED: Completada exitosamente
     * - FAILED: Falló completamente
     * - PARTIAL: Completada parcialmente con errores
     * - CANCELLED: Cancelada por el usuario
     */
    @Column(nullable = false, length = 20)
    val status: String = Status.RUNNING,

    @Column(name = "total_records", nullable = false)
    val totalRecords: Int = 0,

    @Column(name = "successful_records", nullable = false)
    val successfulRecords: Int = 0,

    @Column(name = "failed_records", nullable = false)
    val failedRecords: Int = 0,

    @Column(name = "skipped_records", nullable = false)
    val skippedRecords: Int = 0,

    /**
     * Resumen de errores agrupados en formato JSONB.
     * Ejemplo: {"validation_error": 15, "duplicate_key": 3, "timeout": 1}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_summary", columnDefinition = "jsonb")
    val errorSummary: String? = null,

    /**
     * Detalles completos de errores para debugging.
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    val errorDetails: String? = null,

    /**
     * Duración de la operación en segundos.
     * Calculado como: completedAt - startedAt
     */
    @Column(name = "duration_seconds")
    val durationSeconds: Int? = null,

    /**
     * Throughput: registros procesados por segundo.
     * Calculado como: totalRecords / durationSeconds
     */
    @Column(name = "records_per_second", precision = 10, scale = 2)
    val recordsPerSecond: BigDecimal? = null,

    /**
     * Archivo de origen (para operaciones de import/export).
     */
    @Column(name = "source_file", length = 255)
    val sourceFile: String? = null,

    /**
     * Metadata adicional de la operación en formato JSONB.
     * Ejemplo: {"import_format": "CSV", "delimiter": ",", "encoding": "UTF-8"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val metadata: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con Tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    var tenant: Tenant? = null

    /**
     * Relación ManyToOne con User (usuario que disparó la operación).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by", referencedColumnName = "id", insertable = false, updatable = false)
    var user: User? = null

    override fun toString(): String {
        return "BulkOperationLog(id=$id, operationType='$operationType', targetTable='$targetTable', status='$status', totalRecords=$totalRecords, successfulRecords=$successfulRecords, failedRecords=$failedRecords)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BulkOperationLog) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object Status {
            const val RUNNING = "RUNNING"
            const val COMPLETED = "COMPLETED"
            const val FAILED = "FAILED"
            const val PARTIAL = "PARTIAL"
            const val CANCELLED = "CANCELLED"
        }

        object OperationType {
            const val IMPORT = "IMPORT"
            const val EXPORT = "EXPORT"
            const val MIGRATION = "MIGRATION"
            const val BATCH_UPDATE = "BATCH_UPDATE"
            const val BATCH_DELETE = "BATCH_DELETE"
        }
    }
}

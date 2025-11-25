package com.apptolast.invernaderos.features.audit

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID
import com.apptolast.invernaderos.features.user.User

/**
 * Entity para registro de auditoría de cambios en el sistema.
 * Almacena todos los cambios (INSERT, UPDATE, DELETE) en tablas críticas.
 *
 * @property id ID único del registro de auditoría (BIGSERIAL)
 * @property tableName Nombre de la tabla donde ocurrió el cambio
 * @property recordId UUID del registro modificado
 * @property operation Tipo de operación: INSERT, UPDATE, DELETE
 * @property oldValues Valores antiguos en formato JSONB (solo para UPDATE y DELETE)
 * @property newValues Valores nuevos en formato JSONB (solo para INSERT y UPDATE)
 * @property changedFields Array de campos que cambiaron (solo para UPDATE)
 * @property changedBy UUID del usuario que realizó el cambio
 * @property changedByUsername Nombre de usuario que realizó el cambio (denormalizado)
 * @property changedAt Timestamp del cambio
 * @property changeReason Razón del cambio (opcional, para cambios significativos)
 * @property ipAddress Dirección IP desde donde se realizó el cambio
 * @property userAgent User-Agent del cliente que realizó el cambio
 * @property sessionId ID de sesión del usuario
 * @property applicationVersion Versión de la aplicación que realizó el cambio
 * @property createdAt Fecha de creación del registro de auditoría
 */
@Entity
@Table(
    name = "audit_log",
    schema = "metadata",
    indexes = [
        Index(name = "idx_audit_log_table_name", columnList = "table_name"),
        Index(name = "idx_audit_log_record_id", columnList = "record_id"),
        Index(name = "idx_audit_log_operation", columnList = "operation"),
        Index(name = "idx_audit_log_changed_by", columnList = "changed_by"),
        Index(name = "idx_audit_log_changed_at", columnList = "changed_at"),
        Index(name = "idx_audit_log_table_record", columnList = "table_name, record_id, changed_at")
    ]
)
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "table_name", nullable = false, length = 100)
    val tableName: String,

    @Column(name = "record_id", nullable = false)
    val recordId: UUID,

    /**
     * Tipo de operación: INSERT, UPDATE, DELETE
     */
    @Column(nullable = false, length = 10)
    val operation: String,

    /**
     * Valores antiguos del registro en formato JSONB.
     * Para UPDATE: valores antes del cambio
     * Para DELETE: valores antes de eliminar
     * Para INSERT: NULL
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    val oldValues: String? = null,

    /**
     * Valores nuevos del registro en formato JSONB.
     * Para UPDATE: valores después del cambio
     * Para INSERT: valores del nuevo registro
     * Para DELETE: NULL
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    val newValues: String? = null,

    // ⚠️ Campo changed_fields es text[] (PostgreSQL ARRAY)
    // NO lo mapeamos porque causa problemas con Hibernate validation
    // Si lo necesitas, agregar: implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")
    // y usar @Type(io.hypersistence.utils.hibernate.type.array.ListArrayType::class)

    /**
     * UUID del usuario que realizó el cambio.
     * NULL si fue un cambio del sistema.
     */
    @Column(name = "changed_by")
    val changedBy: UUID? = null,

    /**
     * Nombre de usuario que realizó el cambio (denormalizado para consultas rápidas).
     */
    @Column(name = "changed_by_username", length = 100)
    val changedByUsername: String? = null,

    @Column(name = "changed_at", nullable = false)
    val changedAt: Instant = Instant.now(),

    /**
     * Razón del cambio (opcional).
     * Útil para cambios significativos que requieren justificación.
     */
    @Column(name = "change_reason", columnDefinition = "TEXT")
    val changeReason: String? = null,

    /**
     * Dirección IP desde donde se realizó el cambio.
     */
    @Column(name = "ip_address", columnDefinition = "INET")
    val ipAddress: String? = null,

    /**
     * User-Agent del cliente que realizó el cambio.
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null,

    /**
     * ID de sesión del usuario.
     */
    @Column(name = "session_id", length = 100)
    val sessionId: String? = null,

    /**
     * Versión de la aplicación que realizó el cambio.
     */
    @Column(name = "application_version", length = 50)
    val applicationVersion: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con User (usuario que realizó el cambio).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", referencedColumnName = "id", insertable = false, updatable = false)
    var user: User? = null

    override fun toString(): String {
        return "AuditLog(id=$id, tableName='$tableName', recordId=$recordId, operation='$operation', changedBy=$changedBy, changedAt=$changedAt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuditLog) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object Operation {
            const val INSERT = "INSERT"
            const val UPDATE = "UPDATE"
            const val DELETE = "DELETE"
        }
    }
}

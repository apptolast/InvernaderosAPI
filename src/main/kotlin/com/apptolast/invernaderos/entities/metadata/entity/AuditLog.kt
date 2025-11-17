package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.InetAddress
import java.time.Instant
import java.util.UUID

/**
 * Entidad READONLY para auditoría completa de cambios en tablas críticas.
 * Registra INSERT, UPDATE, DELETE con snapshots completos before/after.
 *
 * ⚠️ IMPORTANTE: Esta es una entidad de solo lectura (@Immutable).
 * Los registros se crean mediante triggers de base de datos.
 *
 * @property id ID autoincremental del registro de auditoría
 * @property tableName Nombre de la tabla donde ocurrió el cambio
 * @property recordId UUID del registro modificado
 * @property operation Tipo de operación: INSERT, UPDATE, DELETE
 * @property oldValues Snapshot JSONB del estado antes del cambio (NULL para INSERT)
 * @property newValues Snapshot JSONB del estado después del cambio (NULL para DELETE)
 * @property changedFields Array de nombres de campos que cambiaron (solo para UPDATE)
 * @property changedBy UUID del usuario que hizo el cambio
 * @property changedByUsername Username denormalizado para registro histórico
 * @property changedAt Timestamp del cambio
 * @property changeReason Razón del cambio (opcional)
 * @property ipAddress IP desde donde se hizo el cambio
 * @property userAgent User agent del cliente
 * @property sessionId ID de sesión
 * @property applicationVersion Versión de la aplicación que hizo el cambio
 * @property createdAt Timestamp de creación del registro de auditoría
 */
@Entity
@Table(name = "audit_log", schema = "metadata")
@Immutable
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "table_name", nullable = false, length = 100)
    val tableName: String,

    @Column(name = "record_id", nullable = false)
    val recordId: UUID,

    @Column(name = "operation", nullable = false, length = 10)
    val operation: String,  // INSERT, UPDATE, DELETE

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    val oldValues: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    val newValues: String? = null,

    @Column(name = "changed_fields", columnDefinition = "text[]")
    val changedFields: Array<String>? = null,

    @Column(name = "changed_by")
    val changedBy: UUID? = null,

    @Column(name = "changed_by_username", length = 100)
    val changedByUsername: String? = null,

    @Column(name = "changed_at", nullable = false)
    val changedAt: Instant,

    @Column(name = "change_reason", columnDefinition = "TEXT")
    val changeReason: String? = null,

    @Column(name = "ip_address", columnDefinition = "inet")
    val ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null,

    @Column(name = "session_id", length = 100)
    val sessionId: String? = null,

    @Column(name = "application_version", length = 50)
    val applicationVersion: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuditLog) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String {
        return "AuditLog(id=$id, tableName='$tableName', recordId=$recordId, operation='$operation', changedAt=$changedAt)"
    }
}

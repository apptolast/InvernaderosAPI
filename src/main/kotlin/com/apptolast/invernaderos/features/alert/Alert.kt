package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.features.catalog.AlertSeverity
import com.apptolast.invernaderos.features.catalog.AlertType
import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import com.apptolast.invernaderos.features.user.User
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

/**
 * Entity que representa una Alerta del sistema de invernaderos.
 * Las alertas se generan por eventos criticos: sensores offline, umbrales excedidos, etc.
 *
 * @property id ID unico de la alerta (BIGINT auto-generado)
 * @property greenhouseId ID del invernadero donde ocurrio la alerta
 * @property tenantId ID del tenant (denormalizado para queries optimizados)
 * @property alertTypeId FK al tipo de alerta (alert_types)
 * @property severityId FK a la severidad (alert_severities)
 * @property message Mensaje descriptivo de la alerta
 * @property isResolved Si la alerta fue resuelta
 * @property resolvedAt Timestamp cuando se resolvio
 * @property resolvedByUserId ID del usuario que resolvio
 * @property createdAt Timestamp de creacion
 * @property updatedAt Timestamp de ultima actualizacion
 */
@NamedEntityGraph(
    name = "Alert.context",
    attributeNodes = [
        NamedAttributeNode("tenant"),
        NamedAttributeNode("greenhouse"),
        NamedAttributeNode("resolvedByUser"),
        NamedAttributeNode("alertType"),
        NamedAttributeNode("severity")
    ]
)
@Entity
@Table(
    name = "alerts",
    schema = "metadata",
    indexes = [
        Index(name = "idx_alerts_tenant", columnList = "tenant_id"),
        Index(name = "idx_alerts_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_alerts_resolved", columnList = "is_resolved"),
        Index(name = "idx_alerts_created_at", columnList = "created_at"),
        Index(name = "idx_alerts_alert_type_id", columnList = "alert_type_id"),
        Index(name = "idx_alerts_severity_id", columnList = "severity_id"),
        Index(name = "idx_alerts_tenant_unresolved", columnList = "tenant_id, is_resolved, created_at"),
        Index(name = "idx_alerts_unresolved", columnList = "is_resolved, created_at"),
        Index(name = "idx_alerts_greenhouse_severity_status", columnList = "greenhouse_id, severity_id, is_resolved, created_at")
    ]
)
data class Alert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @field:NotNull(message = "Greenhouse ID is required")
    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: Long,

    @field:NotNull(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long,

    /**
     * FK al tipo de alerta (alert_types).
     * Nullable para compatibilidad con datos legacy.
     */
    @Column(name = "alert_type_id")
    val alertTypeId: Short? = null,

    /**
     * FK a la severidad (alert_severities).
     * Nullable para compatibilidad con datos legacy.
     */
    @Column(name = "severity_id")
    val severityId: Short? = null,

    /**
     * Mensaje descriptivo de la alerta.
     */
    @field:NotBlank(message = "Message is required")
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    val message: String,

    /**
     * Indica si la alerta fue resuelta.
     */
    @Column(name = "is_resolved", nullable = false)
    var isResolved: Boolean = false,

    /**
     * Timestamp cuando se resolvio la alerta.
     * NULL si aun no esta resuelta.
     */
    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null,

    /**
     * ID del usuario que resolvio la alerta.
     */
    @Column(name = "resolved_by_user_id")
    var resolvedByUserId: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "tenant_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var tenant: Tenant? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "greenhouse_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var greenhouse: Greenhouse? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "resolved_by_user_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var resolvedByUser: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "alert_type_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var alertType: AlertType? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "severity_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var severity: AlertSeverity? = null

    override fun toString(): String {
        return "Alert(id=$id, alertTypeId=$alertTypeId, severityId=$severityId, message='${message.take(50)}...', isResolved=$isResolved, greenhouseId=$greenhouseId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alert) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

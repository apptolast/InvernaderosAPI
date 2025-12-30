package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import com.apptolast.invernaderos.features.user.User
import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa una Alerta del sistema de invernaderos. Las alertas se generan por eventos
 * criticos: sensores offline, umbrales excedidos, fallos de actuadores, etc.
 *
 * @property id ID unico de la alerta (UUID PK)
 * @property tenantId UUID del tenant (denormalizado para queries optimizados)
 * @property greenhouseId UUID del invernadero donde ocurrio la alerta
 * @property deviceId UUID del dispositivo relacionado (nullable)
 * @property alertType Tipo de alerta (THRESHOLD, OFFLINE, ERROR, SYSTEM, etc.)
 * @property severity Severidad (INFO, WARNING, ERROR, CRITICAL)
 * @property message Mensaje descriptivo de la alerta
 * @property alertData Datos adicionales en formato JSONB (ej: {"threshold": 30, "current_value": 35})
 * @property isResolved Si la alerta fue resuelta
 * @property resolvedAt Timestamp cuando se resolvio
 * @property resolvedBy Usuario que resolvio (VARCHAR legacy)
 * @property resolvedByUserId UUID del usuario que resolvio
 * @property createdAt Timestamp de creacion
 * @property updatedAt Timestamp de ultima actualizacion
 */
@NamedEntityGraph(
        name = "Alert.context",
        attributeNodes =
                [
                        NamedAttributeNode("tenant"),
                        NamedAttributeNode("greenhouse"),
                        NamedAttributeNode("resolvedByUser")]
)
@Entity
@Table(
        name = "alerts",
        schema = "metadata",
        indexes =
                [
                        Index(name = "idx_alerts_tenant", columnList = "tenant_id"),
                        Index(
                                name = "idx_alerts_tenant_unresolved",
                                columnList = "tenant_id, is_resolved, created_at"
                        ),
                        Index(
                                name = "idx_alerts_unresolved",
                                columnList = "is_resolved, created_at"
                        ),
                        Index(name = "idx_alerts_severity", columnList = "severity, created_at"),
                        Index(name = "idx_alerts_type", columnList = "alert_type, created_at"),
                        Index(name = "idx_alerts_created_at", columnList = "created_at"),
                        Index(name = "idx_alerts_device", columnList = "device_id"),
                        Index(
                                name = "idx_alerts_greenhouse_severity_status",
                                columnList = "greenhouse_id, severity, is_resolved, created_at"
                        )]
)
data class Alert(
        @Id @Column(name = "id", columnDefinition = "UUID") val id: UUID = UUID.randomUUID(),

        /**
         * Tenant ID denormalizado para queries directos sin JOIN. Mejora performance en queries
         * multi-tenant.
         */
        @field:NotNull(message = "Tenant ID is required")
        @Column(name = "tenant_id", nullable = false)
        val tenantId: UUID,
        @field:NotNull(message = "Greenhouse ID is required")
        @Column(name = "greenhouse_id", nullable = false)
        val greenhouseId: UUID,

        /** Dispositivo relacionado (nullable - no todas las alertas son de dispositivos) */
        @Column(name = "device_id") val deviceId: UUID? = null,

        /**
         * Tipo de alerta. Valores validos (CHECK constraint):
         * THRESHOLD, THRESHOLD_EXCEEDED, OFFLINE, SENSOR_OFFLINE, ERROR, SYSTEM, WARNING, INFO, ACTUATOR_FAILURE
         */
        @field:NotBlank(message = "Alert type is required")
        @field:Size(max = 50, message = "Alert type must not exceed 50 characters")
        @field:Pattern(
                regexp = "^[A-Z_]+$",
                message =
                        "Invalid alert type. Must be uppercase letters and underscores only (e.g., SENSOR_OFFLINE, THRESHOLD_EXCEEDED, ACTUATOR_FAILURE)"
        )
        @Column(name = "alert_type", length = 50, nullable = false)
        val alertType: String,

        /**
         * Severidad de la alerta. Valores validos (CHECK constraint):
         * INFO, WARNING, ERROR, CRITICAL
         */
        @field:NotBlank(message = "Severity is required")
        @field:Size(max = 20, message = "Severity must not exceed 20 characters")
        @field:Pattern(
                regexp = "^(INFO|WARNING|ERROR|CRITICAL)$",
                message =
                        "Invalid severity level. Must be one of: INFO, WARNING, ERROR, CRITICAL"
        )
        @Column(name = "severity", length = 20, nullable = false)
        val severity: String,

        /**
         * Mensaje descriptivo de la alerta. Ejemplo: "Temperatura excede umbral máximo: 35°C
         * (límite: 30°C)" Limitado a 1000 caracteres para mantener mensajes concisos y optimizar
         * rendimiento.
         */
        @field:NotBlank(message = "Message is required")
        @field:Size(max = 1000, message = "Message must not exceed 1000 characters")
        @Column(name = "message", length = 1000, nullable = false)
        val message: String,

        /**
         * Datos adicionales en formato JSONB. Ejemplo: {"threshold": 30, "current_value": 35,
         * "sensor_code": "TEMP01"}
         *
         * Limitado a 10,000 caracteres. Para estructuras JSON complejas, considere:
         * - Mantener estructuras planas cuando sea posible
         * - Evitar arrays grandes o anidamiento profundo
         * - Almacenar datos históricos en tablas separadas si excede este límite
         */
        @field:Size(max = 10000, message = "Alert data must not exceed 10000 characters")
        @Column(name = "alert_data", columnDefinition = "jsonb")
        val alertData: String? = null,

        /** Indica si la alerta fue resuelta. Default: false */
        @Column(name = "is_resolved", nullable = false) var isResolved: Boolean = false,

        /** Timestamp cuando se resolvió la alerta. NULL si aún no está resuelta. */
        @Column(name = "resolved_at") var resolvedAt: Instant? = null,

        /**
         * Usuario que resolvió la alerta (VARCHAR legacy). DEPRECATED: Usar resolvedByUserId en su
         * lugar
         */
        @field:Size(max = 100, message = "Resolved by must not exceed 100 characters")
        @Column(name = "resolved_by", length = 100)
        var resolvedBy: String? = null,

        /**
         * UUID del usuario que resolvió la alerta (normalizado). References: metadata.users.id
         * Agregado en V10 para normalización
         */
        @Column(name = "resolved_by_user_id") var resolvedByUserId: UUID? = null,
        @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
        @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now()
) {
    /** Relación ManyToOne con Tenant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tenant_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    var tenant: Tenant? = null

    /** Relacion ManyToOne con Greenhouse. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "greenhouse_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    var greenhouse: Greenhouse? = null

    /** Relacion ManyToOne con User (usuario que resolvio). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "resolved_by_user_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    var resolvedByUser: User? = null

    override fun toString(): String {
        return "Alert(id=$id, alertType='$alertType', severity='$severity', message='$message', isResolved=$isResolved, greenhouseId=$greenhouseId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alert) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}

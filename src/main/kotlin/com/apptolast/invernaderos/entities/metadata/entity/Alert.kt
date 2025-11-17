package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa una Alerta del sistema de invernaderos.
 * Las alertas se generan por eventos críticos: sensores offline, umbrales excedidos, fallos de actuadores, etc.
 *
 * @property id ID único de la alerta (UUID PK)
 * @property tenantId UUID del tenant (denormalizado para queries optimizados)
 * @property greenhouseId UUID del invernadero donde ocurrió la alerta
 * @property sensorId UUID del sensor relacionado (nullable)
 * @property alertType Tipo de alerta (VARCHAR legacy: THRESHOLD_EXCEEDED, SENSOR_OFFLINE, etc.)
 * @property alertTypeId ID del tipo de alerta normalizado (SMALLINT, references alert_types)
 * @property severity Severidad (VARCHAR legacy: INFO, WARNING, ERROR, CRITICAL)
 * @property severityId ID de severidad normalizado (SMALLINT, references alert_severities)
 * @property message Mensaje descriptivo de la alerta
 * @property alertData Datos adicionales en formato JSONB (ej: {"threshold": 30, "current_value": 35})
 * @property isResolved Si la alerta fue resuelta
 * @property resolvedAt Timestamp cuando se resolvió
 * @property resolvedBy Usuario que resolvió (VARCHAR legacy)
 * @property resolvedByUserId UUID del usuario que resolvió (normalizado)
 * @property createdAt Timestamp de creación
 * @property updatedAt Timestamp de última actualización
 */
@Entity
@Table(
    name = "alerts",
    schema = "metadata",
    indexes = [
        Index(name = "idx_alerts_tenant", columnList = "tenant_id"),
        Index(name = "idx_alerts_tenant_unresolved", columnList = "tenant_id, is_resolved, created_at"),
        Index(name = "idx_alerts_unresolved", columnList = "is_resolved, created_at"),
        Index(name = "idx_alerts_severity", columnList = "severity, created_at"),
        Index(name = "idx_alerts_type", columnList = "alert_type, created_at"),
        Index(name = "idx_alerts_created_at", columnList = "created_at"),
        Index(name = "idx_alerts_alert_type_id", columnList = "alert_type_id"),
        Index(name = "idx_alerts_severity_id", columnList = "severity_id"),
        Index(name = "idx_alerts_greenhouse_severity_status", columnList = "greenhouse_id, severity_id, is_resolved, created_at")
    ]
)
data class Alert(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    /**
     * Tenant ID denormalizado para queries directos sin JOIN.
     * Mejora performance en queries multi-tenant.
     */
    @field:NotNull(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @field:NotNull(message = "Greenhouse ID is required")
    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

    /**
     * Sensor relacionado (nullable - no todas las alertas son de sensores)
     */
    @Column(name = "sensor_id")
    val sensorId: UUID? = null,

    /**
     * Tipo de alerta (VARCHAR legacy).
     * Valores: SENSOR_OFFLINE, ACTUATOR_OFFLINE, THRESHOLD_EXCEEDED_HIGH, THRESHOLD_EXCEEDED_LOW,
     * CRITICAL_THRESHOLD_HIGH, CRITICAL_THRESHOLD_LOW, ACTUATOR_ERROR, SENSOR_ERROR,
     * CONNECTIVITY_LOST, BATTERY_LOW, MAINTENANCE_DUE, CALIBRATION_REQUIRED, DATA_ANOMALY, etc.
     * DEPRECATED: Usar alertTypeId en su lugar
     */
    @field:NotBlank(message = "Alert type is required")
    @field:Size(max = 50, message = "Alert type must not exceed 50 characters")
    @field:Pattern(
        regexp = "^[A-Z_]+$",
        message = "Invalid alert type. Must be uppercase letters and underscores only (e.g., SENSOR_OFFLINE, THRESHOLD_EXCEEDED_HIGH, BATTERY_LOW)"
    )
    @Column(name = "alert_type", length = 50, nullable = false)
    val alertType: String,

    /**
     * ID del tipo de alerta normalizado (SMALLINT).
     * References: metadata.alert_types.id
     * Agregado en V13 para normalización
     */
    @Column(name = "alert_type_id", columnDefinition = "SMALLINT")
    val alertTypeId: Short? = null,

    /**
     * Severidad de la alerta (VARCHAR legacy).
     * Valores: INFO, WARNING, ERROR, CRITICAL (o legacy: LOW, MEDIUM, HIGH)
     * DEPRECATED: Usar severityId en su lugar
     */
    @field:NotBlank(message = "Severity is required")
    @field:Size(max = 20, message = "Severity must not exceed 20 characters")
    @field:Pattern(
        regexp = "^(INFO|WARNING|ERROR|CRITICAL|LOW|MEDIUM|HIGH)$",
        message = "Invalid severity level. Must be one of: INFO, WARNING, ERROR, CRITICAL, LOW, MEDIUM, HIGH"
    )
    @Column(name = "severity", length = 20, nullable = false)
    val severity: String,

    /**
     * ID de severidad normalizado (SMALLINT).
     * References: metadata.alert_severities.id
     * Agregado en V13 para normalización
     */
    @Column(name = "severity_id", columnDefinition = "SMALLINT")
    val severityId: Short? = null,

    /**
     * Mensaje descriptivo de la alerta.
     * Ejemplo: "Temperatura excede umbral máximo: 35°C (límite: 30°C)"
     * Limitado a 1000 caracteres para mantener mensajes concisos y optimizar rendimiento.
     */
    @field:NotBlank(message = "Message is required")
    @field:Size(max = 1000, message = "Message must not exceed 1000 characters")
    @Column(name = "message", length = 1000, nullable = false)
    val message: String,

    /**
     * Datos adicionales en formato JSONB.
     * Ejemplo: {"threshold": 30, "current_value": 35, "sensor_code": "TEMP01"}
     * 
     * Limitado a 10,000 caracteres. Para estructuras JSON complejas, considere:
     * - Mantener estructuras planas cuando sea posible
     * - Evitar arrays grandes o anidamiento profundo
     * - Almacenar datos históricos en tablas separadas si excede este límite
     */
    @field:Size(max = 10000, message = "Alert data must not exceed 10000 characters")
    @Column(name = "alert_data", columnDefinition = "jsonb")
    val alertData: String? = null,

    /**
     * Indica si la alerta fue resuelta.
     * Default: false
     */
    @Column(name = "is_resolved", nullable = false)
    val isResolved: Boolean = false,

    /**
     * Timestamp cuando se resolvió la alerta.
     * NULL si aún no está resuelta.
     */
    @Column(name = "resolved_at")
    val resolvedAt: Instant? = null,

    /**
     * Usuario que resolvió la alerta (VARCHAR legacy).
     * DEPRECATED: Usar resolvedByUserId en su lugar
     */
    @field:Size(max = 100, message = "Resolved by must not exceed 100 characters")
    @Column(name = "resolved_by", length = 100)
    val resolvedBy: String? = null,

    /**
     * UUID del usuario que resolvió la alerta (normalizado).
     * References: metadata.users.id
     * Agregado en V10 para normalización
     */
    @Column(name = "resolved_by_user_id")
    val resolvedByUserId: UUID? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Relación ManyToOne con Tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    var tenant: Tenant? = null

    /**
     * Relación ManyToOne con Greenhouse.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenhouse_id", referencedColumnName = "id", insertable = false, updatable = false)
    var greenhouse: Greenhouse? = null

    /**
     * Relación ManyToOne con Sensor (nullable).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", referencedColumnName = "id", insertable = false, updatable = false)
    var sensor: Sensor? = null

    /**
     * Relación ManyToOne con User (usuario que resolvió).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id", referencedColumnName = "id", insertable = false, updatable = false)
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

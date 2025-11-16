package com.apptolast.invernaderos.entities.metadata.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Entidad para catálogo de tipos de alertas
 *
 * Tabla: metadata.alert_types (creada en V12__create_catalog_tables.sql)
 *
 * Categorías: SENSOR, ACTUATOR, SYSTEM, CONNECTIVITY, THRESHOLD
 * Tipos: SENSOR_OFFLINE, THRESHOLD_EXCEEDED_HIGH, CRITICAL_THRESHOLD_HIGH, etc.
 */
@Entity
@Table(name = "alert_types", schema = "metadata")
data class AlertType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "SMALLSERIAL")
    val id: Short? = null,

    /**
     * Nombre del tipo de alerta (ej: "SENSOR_OFFLINE", "THRESHOLD_EXCEEDED_HIGH")
     */
    @Column(name = "name", length = 50, nullable = false, unique = true)
    val name: String,

    /**
     * Descripción del tipo de alerta
     */
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * Severidad por defecto
     */
    @Column(name = "default_severity_id", columnDefinition = "SMALLINT")
    val defaultSeverityId: Short? = null,

    /**
     * Categoría: SENSOR, ACTUATOR, SYSTEM, CONNECTIVITY, THRESHOLD
     */
    @Column(name = "category", length = 30, nullable = false)
    val category: String,

    /**
     * Indica si el tipo está activo
     */
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    /**
     * Fecha de creación
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

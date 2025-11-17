package com.apptolast.invernaderos.entities.metadata.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Entidad para catálogo de niveles de severidad de alertas
 *
 * Tabla: metadata.alert_severities (creada en V12__create_catalog_tables.sql)
 *
 * Niveles: INFO (1), WARNING (2), ERROR (3), CRITICAL (4)
 */
@Entity
@Table(name = "alert_severities", schema = "metadata")
data class AlertSeverity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "SMALLSERIAL")
    val id: Short? = null,

    /**
     * Nombre del nivel de severidad (ej: "INFO", "WARNING", "CRITICAL")
     */
    @Column(name = "name", length = 20, nullable = false, unique = true)
    val name: String,

    /**
     * Nivel numérico (1=INFO, 2=WARNING, 3=ERROR, 4=CRITICAL)
     */
    @Column(name = "level", columnDefinition = "SMALLINT", nullable = false)
    val level: Short,

    /**
     * Descripción del nivel
     */
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * Color para UI (hex code: #RRGGBB)
     */
    @Column(name = "color", length = 7)
    val color: String? = null,

    /**
     * Indica si requiere acción inmediata
     */
    @Column(name = "requires_action", nullable = false)
    val requiresAction: Boolean = false,

    /**
     * Minutos de delay antes de notificar
     * IMPORTANTE: DB tiene INTEGER (4 bytes) en lugar de SMALLINT (2 bytes)
     * Error en migration V12 línea 187 - usa INT en lugar de SMALLINT
     * Trade-off: Acepta usar más espacio para evitar mismatch de schema validation
     */
    @Column(name = "notification_delay_minutes", columnDefinition = "INTEGER")
    val notificationDelayMinutes: Short = 0,

    /**
     * Fecha de creación
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

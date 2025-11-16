package com.apptolast.invernaderos.entities.metadata.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Entidad para catálogo de estados de actuadores
 *
 * Tabla: metadata.actuator_states (creada en V12__create_catalog_tables.sql)
 *
 * Estados: OFF, ON, AUTO, MANUAL, ERROR, MAINTENANCE, STANDBY, CALIBRATING, OFFLINE
 */
@Entity
@Table(name = "actuator_states", schema = "metadata")
data class ActuatorState(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "SMALLSERIAL")
    val id: Short? = null,

    /**
     * Nombre del estado (ej: "OFF", "ON", "AUTO")
     */
    @Column(name = "name", length = 20, nullable = false, unique = true)
    val name: String,

    /**
     * Descripción del estado
     */
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * Indica si el estado es operacional (true para ON, AUTO, MANUAL)
     */
    @Column(name = "is_operational", nullable = false)
    val isOperational: Boolean = false,

    /**
     * Orden de visualización
     */
    @Column(name = "display_order", columnDefinition = "SMALLINT", nullable = false)
    val displayOrder: Short = 0,

    /**
     * Color para UI (hex code: #RRGGBB)
     */
    @Column(name = "color", length = 7)
    val color: String? = null,

    /**
     * Fecha de creación
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

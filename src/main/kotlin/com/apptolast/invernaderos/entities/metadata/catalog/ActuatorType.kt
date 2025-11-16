package com.apptolast.invernaderos.entities.metadata.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Entidad para catálogo de tipos de actuadores
 *
 * Tabla: metadata.actuator_types (creada en V12__create_catalog_tables.sql)
 *
 * Tipos: VENTILATOR, HEATER, COOLER, IRRIGATOR, LIGHT, WINDOW, PUMP, VALVE, etc.
 * Control types: BINARY (ON/OFF), CONTINUOUS (0-100%), MULTI_STATE (LOW/MEDIUM/HIGH)
 */
@Entity
@Table(name = "actuator_types", schema = "metadata")
data class ActuatorType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "SMALLSERIAL")
    val id: Short? = null,

    /**
     * Nombre del tipo de actuador (ej: "VENTILATOR", "HEATER")
     */
    @Column(name = "name", length = 30, nullable = false, unique = true)
    val name: String,

    /**
     * Descripción del tipo de actuador
     */
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * Unidad por defecto para este tipo de actuador
     */
    @Column(name = "default_unit_id", columnDefinition = "SMALLINT")
    val defaultUnitId: Short? = null,

    /**
     * Tipo de control: BINARY, CONTINUOUS, MULTI_STATE
     */
    @Column(name = "control_type", length = 20, nullable = false)
    val controlType: String = "BINARY",

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

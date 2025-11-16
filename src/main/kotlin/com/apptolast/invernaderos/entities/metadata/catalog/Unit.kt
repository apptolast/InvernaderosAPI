package com.apptolast.invernaderos.entities.metadata.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Entidad para catálogo de unidades de medida
 *
 * Tabla: metadata.units (creada en V12__create_catalog_tables.sql)
 *
 * Propósito: Normalizar el campo 'unit' en sensors/actuators
 * Reducción: de 25 bytes (VARCHAR) a 2 bytes (SMALLINT) por registro
 *
 * Ejemplos: °C, °F, %, lux, hPa, ppm, W/m², m/s, mm
 */
@Entity
@Table(name = "units", schema = "metadata")
data class Unit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "SMALLSERIAL")
    val id: Short? = null,

    /**
     * Símbolo de la unidad (ej: "°C", "%", "ppm")
     */
    @Column(name = "symbol", length = 10, nullable = false, unique = true)
    val symbol: String,

    /**
     * Nombre descriptivo de la unidad (ej: "Grados Celsius", "Porcentaje")
     */
    @Column(name = "name", length = 30, nullable = false)
    val name: String,

    /**
     * Descripción opcional
     */
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * Indica si la unidad está activa
     */
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    /**
     * Fecha de creación
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

package com.apptolast.invernaderos.features.catalog.catalog

import jakarta.persistence.*
import java.time.Instant

/**
 * Entidad para catálogo de tipos de sensores
 *
 * Tabla: metadata.sensor_types (creada en V12__create_catalog_tables.sql)
 *
 * Propósito: Normalizar el campo 'sensor_type' en sensors
 * Reducción: de 30+ bytes (VARCHAR) a 2 bytes (SMALLINT) por registro
 * Ahorro: ~280 MB en 10M registros
 *
 * Tipos: TEMPERATURE, HUMIDITY, LIGHT, SOIL_MOISTURE, CO2, PRESSURE, etc.
 */
@Entity
@Table(name = "sensor_types", schema = "metadata")
data class SensorType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "SMALLSERIAL")
    val id: Short? = null,

    /**
     * Nombre del tipo de sensor (ej: "TEMPERATURE", "HUMIDITY")
     */
    @Column(name = "name", length = 30, nullable = false, unique = true)
    val name: String,

    /**
     * Descripción del tipo de sensor
     */
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * Unidad por defecto para este tipo de sensor
     */
    @Column(name = "default_unit_id", columnDefinition = "SMALLINT")
    val defaultUnitId: Short? = null,

    /**
     * Tipo de dato (DECIMAL, INTEGER, BOOLEAN, TEXT)
     */
    @Column(name = "data_type", length = 20, nullable = false)
    val dataType: String = "DECIMAL",

    /**
     * Valor mínimo esperado
     * IMPORTANTE: PostgreSQL reporta NUMERIC(10,2), no DECIMAL(10,2)
     * Ambos son sinónimos en PostgreSQL, pero Hibernate valida el string literal
     */
    @Column(name = "min_value", columnDefinition = "NUMERIC(10,2)")
    val minValue: Double? = null,

    /**
     * Valor máximo esperado
     * IMPORTANTE: PostgreSQL reporta NUMERIC(10,2), no DECIMAL(10,2)
     * Ambos son sinónimos en PostgreSQL, pero Hibernate valida el string literal
     */
    @Column(name = "max_value", columnDefinition = "NUMERIC(10,2)")
    val maxValue: Double? = null,

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

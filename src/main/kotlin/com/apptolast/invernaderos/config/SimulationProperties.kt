package com.apptolast.invernaderos.config

import jakarta.validation.constraints.Pattern
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

/**
 * Propiedades de configuración para la simulación de datos de invernadero
 *
 * Esta clase agrupa todas las configuraciones relacionadas con la simulación
 * siguiendo las mejores prácticas de Spring Boot (CLAUDE.md line 59).
 *
 * Configuración en application.yaml:
 * ```yaml
 * greenhouse:
 *   simulation:
 *     enabled: false          # Activar/desactivar simulación
 *     interval-ms: 5000       # Intervalo en milisegundos entre generaciones
 *     greenhouse-id: "001"    # ID del invernadero a simular
 * ```
 */
@Configuration
@ConfigurationProperties(prefix = "greenhouse.simulation")
@Validated
data class SimulationProperties(
    /**
     * Si la simulación está habilitada
     */
    var enabled: Boolean = false,

    /**
     * Intervalo en milisegundos entre generaciones automáticas de datos
     */
    var intervalMs: Int = 5000,

    /**
     * ID del tenant para el cual se generan datos simulados (mqtt_topic_prefix)
     * Debe coincidir con un tenant existente en la base de datos
     * Ejemplos válidos: SARA, DEFAULT, HORTAMED, ELPRADO, 001
     */
    @get:Pattern(regexp = "^[A-Z0-9_]{1,20}$", message = "Tenant ID must be alphanumeric (uppercase letters, numbers, underscores), 1-20 characters")
    var greenhouseId: String = "SARA"
)

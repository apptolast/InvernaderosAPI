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
     * ID del invernadero para el cual se generan datos simulados
     */
    @get:Pattern(regexp = "^[0-9]{3}$", message = "Greenhouse ID must be a 3-digit number")
    var greenhouseId: String = "001"
)

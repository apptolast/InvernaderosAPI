package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para actualizar un tipo de alerta existente.
 */
@Schema(description = "Request para actualizar un tipo de alerta")
data class AlertTypeUpdateRequest(
    @Schema(description = "Nuevo nombre del tipo (máx 30 caracteres)", example = "MAINTENANCE_REQUIRED_V2")
    @field:jakarta.validation.constraints.Size(max = 30, message = "El nombre no puede exceder 30 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripción del tipo de alerta", example = "Se requiere mantenimiento preventivo del equipo")
    val description: String? = null
)

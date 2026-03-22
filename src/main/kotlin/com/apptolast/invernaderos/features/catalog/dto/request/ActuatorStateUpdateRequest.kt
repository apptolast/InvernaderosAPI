package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para actualizar un estado de actuador existente.
 */
@Schema(description = "Request para actualizar un estado de actuador")
data class ActuatorStateUpdateRequest(
    @Schema(description = "Nuevo nombre del estado (max 20 caracteres)", example = "WARMING_UP_V2")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripcion del estado", example = "Calentando el sistema - version 2")
    val description: String? = null,

    @Schema(description = "Nuevo estado operacional", example = "true")
    val isOperational: Boolean? = null,

    @Schema(description = "Nuevo orden para mostrar en UI", example = "6")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El orden no puede ser negativo")
    val displayOrder: Short? = null,

    @Schema(description = "Nuevo color hexadecimal para UI", example = "#FFA500")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color debe ser un codigo hexadecimal valido (ej: #FF0000)"
    )
    val color: String? = null
)

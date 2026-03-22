package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para actualizar un periodo existente.
 */
@Schema(description = "Request para actualizar un periodo")
data class PeriodUpdateRequest(
    @Schema(description = "Nuevo nombre del periodo (máx 10 caracteres)", example = "MORNING_V2")
    @field:jakarta.validation.constraints.Size(max = 10, message = "El nombre no puede exceder 10 caracteres")
    val name: String?
)

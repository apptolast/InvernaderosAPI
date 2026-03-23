package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para actualizar una categoría de dispositivo existente.
 */
@Schema(description = "Request para actualizar una categoría de dispositivo")
data class DeviceCategoryUpdateRequest(
    @Schema(description = "Nuevo nombre de la categoría (único, máx 20 caracteres)", example = "HYBRID_UPDATED")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String?
)

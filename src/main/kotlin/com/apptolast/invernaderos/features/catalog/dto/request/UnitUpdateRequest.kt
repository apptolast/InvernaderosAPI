package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para actualizar una unidad de medida existente.
 */
@Schema(description = "Request para actualizar una unidad de medida")
data class UnitUpdateRequest(
    @Schema(description = "Nuevo símbolo de la unidad (máx 10 caracteres)", example = "mg/L")
    @field:jakarta.validation.constraints.Size(max = 10, message = "El símbolo no puede exceder 10 caracteres")
    val symbol: String? = null,

    @Schema(description = "Nuevo nombre de la unidad (máx 50 caracteres)", example = "Miligramos por litro")
    @field:jakarta.validation.constraints.Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripción de la unidad", example = "Concentración de nutrientes en solución")
    val description: String? = null,

    @Schema(description = "Nuevo estado activo/inactivo", example = "true")
    val isActive: Boolean? = null
)

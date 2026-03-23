package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para actualizar un tipo de dato existente.
 */
@Schema(description = "Request para actualizar un tipo de dato")
data class DataTypeUpdateRequest(
    @Schema(description = "Nuevo nombre del tipo (max 20 caracteres)", example = "PERCENTAGE_V2")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripcion del tipo", example = "Valor porcentual 0-100 con decimales")
    val description: String? = null,

    @Schema(description = "Nueva expresion regular para validar valores", example = "^\\d{1,3}(\\.\\d+)?$")
    val validationRegex: String? = null,

    @Schema(description = "Nuevo ejemplo de valor valido", example = "75.5")
    val exampleValue: String? = null,

    @Schema(description = "Nuevo orden para mostrar en UI", example = "11")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El orden no puede ser negativo")
    val displayOrder: Short? = null,

    @Schema(description = "Nuevo estado activo/inactivo", example = "true")
    val isActive: Boolean? = null
)

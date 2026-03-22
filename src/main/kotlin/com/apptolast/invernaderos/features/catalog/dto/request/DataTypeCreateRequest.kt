package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para crear un nuevo tipo de dato.
 */
@Schema(description = "Request para crear un tipo de dato")
data class DataTypeCreateRequest(
    @Schema(description = "Nombre del tipo (unico, max 20 caracteres)", example = "PERCENTAGE", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String,

    @Schema(description = "Descripcion del tipo", example = "Valor porcentual 0-100")
    val description: String? = null,

    @Schema(description = "Expresion regular para validar valores", example = "^\\d{1,3}$")
    val validationRegex: String? = null,

    @Schema(description = "Ejemplo de valor valido", example = "75")
    val exampleValue: String? = null,

    @Schema(description = "Orden para mostrar en UI", example = "10")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El orden no puede ser negativo")
    val displayOrder: Short? = 0,

    @Schema(description = "Si el tipo esta activo", example = "true")
    val isActive: Boolean? = true
)

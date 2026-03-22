package com.apptolast.invernaderos.features.catalog.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Tipo de dato para valores de configuracion")
data class DataTypeResponse(
    @Schema(description = "ID del tipo de dato", example = "1")
    val id: Short,

    @Schema(description = "Nombre del tipo", example = "INTEGER")
    val name: String,

    @Schema(description = "Descripcion del tipo", example = "Numero entero")
    val description: String?,

    @Schema(description = "Expresion regular para validar valores", example = "^-?\\d+$")
    val validationRegex: String?,

    @Schema(description = "Ejemplo de valor valido", example = "25")
    val exampleValue: String?,

    @Schema(description = "Orden para mostrar en UI", example = "1")
    val displayOrder: Short,

    @Schema(description = "Si el tipo esta activo", example = "true")
    val isActive: Boolean
)

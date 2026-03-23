package com.apptolast.invernaderos.features.catalog.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Unidad de medida")
data class UnitResponse(
    @Schema(description = "ID de la unidad", example = "1")
    val id: Short,

    @Schema(description = "Símbolo de la unidad", example = "°C")
    val symbol: String,

    @Schema(description = "Nombre de la unidad", example = "Grados Celsius")
    val name: String,

    @Schema(description = "Descripción de la unidad", example = "Temperatura en grados Celsius")
    val description: String?,

    @Schema(description = "Si la unidad está activa", example = "true")
    val isActive: Boolean
)

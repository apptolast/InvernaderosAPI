package com.apptolast.invernaderos.features.catalog.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Periodo del día para configuraciones (DAY, NIGHT, ALL)")
data class PeriodResponse(
    @Schema(description = "ID del periodo", example = "1")
    val id: Short,

    @Schema(description = "Nombre del periodo", example = "DAY")
    val name: String
)

package com.apptolast.invernaderos.features.catalog.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Tipo de alerta")
data class AlertTypeResponse(
    @Schema(description = "ID del tipo de alerta", example = "1")
    val id: Short,

    @Schema(description = "Nombre del tipo", example = "THRESHOLD_EXCEEDED")
    val name: String,

    @Schema(description = "Descripción del tipo de alerta")
    val description: String?
)

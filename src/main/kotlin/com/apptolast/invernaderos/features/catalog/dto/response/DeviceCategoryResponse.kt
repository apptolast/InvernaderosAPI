package com.apptolast.invernaderos.features.catalog.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Categoría de dispositivo (SENSOR o ACTUATOR)")
data class DeviceCategoryResponse(
    @Schema(description = "ID de la categoría", example = "1")
    val id: Short,

    @Schema(description = "Nombre de la categoría", example = "SENSOR")
    val name: String
)

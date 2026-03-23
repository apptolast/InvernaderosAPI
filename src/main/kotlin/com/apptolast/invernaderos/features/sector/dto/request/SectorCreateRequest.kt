package com.apptolast.invernaderos.features.sector.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para crear un nuevo Sector")
data class SectorCreateRequest(
    @Schema(description = "ID del invernadero al que pertenece", required = true)
    val greenhouseId: Long,

    @Schema(description = "Variedad o nombre del sector")
    val name: String? = null
)

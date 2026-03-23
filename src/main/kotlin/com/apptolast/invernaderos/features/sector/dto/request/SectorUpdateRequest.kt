package com.apptolast.invernaderos.features.sector.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para actualizar un Sector existente")
data class SectorUpdateRequest(
    @Schema(description = "ID del invernadero al que pertenece (debe pertenecer al mismo tenant)")
    val greenhouseId: Long? = null,

    @Schema(description = "Variedad o nombre del sector")
    val name: String? = null
)

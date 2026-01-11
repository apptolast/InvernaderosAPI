package com.apptolast.invernaderos.features.sector.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Respuesta que representa un Sector")
data class SectorResponse(
    @Schema(description = "ID único del sector") val id: Long,
    @Schema(description = "Código único legible del sector", example = "SEC-00001") val code: String,
    @Schema(description = "ID del invernadero al que pertenece") val greenhouseId: Long,
    @Schema(description = "Variedad o nombre del sector") val variety: String?
)

@Schema(description = "Solicitud para crear un nuevo Sector")
data class SectorCreateRequest(
    @Schema(description = "ID del invernadero al que pertenece") val greenhouseId: Long,
    @Schema(description = "Variedad o nombre del sector") val variety: String? = null
)

@Schema(description = "Solicitud para actualizar un Sector existente")
data class SectorUpdateRequest(
    @Schema(description = "Variedad o nombre del sector") val variety: String? = null
)

fun com.apptolast.invernaderos.features.sector.Sector.toResponse() = SectorResponse(
    id = this.id ?: throw IllegalStateException("Sector ID cannot be null"),
    code = this.code,
    greenhouseId = this.greenhouseId,
    variety = this.variety
)

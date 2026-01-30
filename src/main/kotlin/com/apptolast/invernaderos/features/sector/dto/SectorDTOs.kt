package com.apptolast.invernaderos.features.sector.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Respuesta que representa un Sector")
data class SectorResponse(
    @Schema(description = "ID único del sector") val id: Long,
    @Schema(description = "Código único por tenant del sector", example = "SEC-00001") val code: String,
    @Schema(description = "ID del tenant propietario") val tenantId: Long,
    @Schema(description = "ID del invernadero al que pertenece") val greenhouseId: Long,
    @Schema(description = "Código del invernadero") val greenhouseCode: String?,
    @Schema(description = "Variedad o nombre del sector") val name: String?
)

@Schema(description = "Solicitud para crear un nuevo Sector")
data class SectorCreateRequest(
    @Schema(description = "ID del invernadero al que pertenece", required = true) val greenhouseId: Long,
    @Schema(description = "Variedad o nombre del sector") val name: String? = null
)

@Schema(description = "Solicitud para actualizar un Sector existente")
data class SectorUpdateRequest(
    @Schema(description = "Variedad o nombre del sector") val name: String? = null
)

fun com.apptolast.invernaderos.features.sector.Sector.toResponse() = SectorResponse(
    id = this.id ?: throw IllegalStateException("Sector ID cannot be null"),
    code = this.code,
    tenantId = this.tenantId,
    greenhouseId = this.greenhouseId,
    greenhouseCode = this.greenhouse?.code,
    name = this.name
)

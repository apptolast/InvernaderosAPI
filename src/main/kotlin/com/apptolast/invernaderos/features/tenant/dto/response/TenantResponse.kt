package com.apptolast.invernaderos.features.tenant.dto.response

import com.apptolast.invernaderos.features.tenant.LocationDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Objeto de respuesta que representa un Tenant (Cliente)")
data class TenantResponse(
    @Schema(description = "Identificador único del tenant") val id: Long,
    @Schema(description = "Código único legible del tenant", example = "TNT-00001") val code: String,
    @Schema(description = "Nombre del tenant") val name: String,
    @Schema(description = "Email de contacto") val email: String,
    @Schema(description = "Teléfono de contacto") val phone: String?,
    @Schema(description = "Provincia") val province: String?,
    @Schema(description = "País") val country: String?,
    @Schema(description = "Coordenadas geográficas") val location: LocationDto?,
    @Schema(description = "Si el tenant está activo") val isActive: Boolean?,
    @Schema(description = "Estado para la UI (Activo, Pendiente, Inactivo)") val status: String
)

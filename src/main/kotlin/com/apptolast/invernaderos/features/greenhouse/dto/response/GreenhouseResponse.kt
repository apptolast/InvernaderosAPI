package com.apptolast.invernaderos.features.greenhouse.dto.response

import com.apptolast.invernaderos.features.tenant.LocationDto
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Respuesta que representa un Invernadero")
data class GreenhouseResponse(
    @Schema(description = "ID único del invernadero") val id: Long,
    @Schema(description = "Código único legible del invernadero", example = "GRH-00001") val code: String,
    @Schema(description = "Nombre del invernadero") val name: String,
    @Schema(description = "ID del tenant propietario") val tenantId: Long,
    @Schema(description = "Ubicación geográfica") val location: LocationDto?,
    @Schema(description = "Área en metros cuadrados") val areaM2: BigDecimal?,
    @Schema(description = "Zona horaria") val timezone: String?,
    @Schema(description = "Si el invernadero está activo") val isActive: Boolean,
    @Schema(description = "Fecha de creación") val createdAt: Instant,
    @Schema(description = "Fecha de última actualización") val updatedAt: Instant
)

package com.apptolast.invernaderos.features.greenhouse.dto.request

import com.apptolast.invernaderos.features.tenant.LocationDto
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Solicitud para crear un nuevo Invernadero")
data class GreenhouseCreateRequest(
    @Schema(description = "Nombre del invernadero", example = "Invernadero Principal")
    val name: String,

    @Schema(description = "Ubicación geográfica")
    val location: LocationDto? = null,

    @Schema(description = "Área en metros cuadrados", example = "1500.50")
    val areaM2: BigDecimal? = null,

    @Schema(description = "Zona horaria", example = "Europe/Madrid")
    val timezone: String? = "Europe/Madrid",

    @Schema(description = "Si el invernadero está activo", example = "true")
    val isActive: Boolean? = true
)

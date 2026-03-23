package com.apptolast.invernaderos.features.greenhouse.dto.request

import com.apptolast.invernaderos.features.tenant.LocationDto
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Solicitud para actualizar un Invernadero existente")
data class GreenhouseUpdateRequest(
    @Schema(description = "Nombre del invernadero")
    val name: String? = null,

    @Schema(description = "Ubicación geográfica")
    val location: LocationDto? = null,

    @Schema(description = "Área en metros cuadrados")
    val areaM2: BigDecimal? = null,

    @Schema(description = "Zona horaria")
    val timezone: String? = null,

    @Schema(description = "Si el invernadero está activo")
    val isActive: Boolean? = null
)

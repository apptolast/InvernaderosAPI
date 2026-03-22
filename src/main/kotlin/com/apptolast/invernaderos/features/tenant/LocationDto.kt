package com.apptolast.invernaderos.features.tenant

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para coordenadas geográficas.
 * Compatible con el cliente KMP que envía {lat: Double, lon: Double}
 */
@Schema(description = "Coordenadas geográficas")
data class LocationDto(
    @Schema(description = "Latitud", example = "36.8381")
    val lat: Double? = null,

    @Schema(description = "Longitud", example = "-2.4597")
    val lon: Double? = null
)

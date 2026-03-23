package com.apptolast.invernaderos.features.alert.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para actualizar una Alerta existente")
data class AlertUpdateRequest(
    @Schema(description = "ID del sector donde ocurre la alerta (debe pertenecer al mismo tenant)")
    val sectorId: Long? = null,

    @Schema(description = "ID del tipo de alerta")
    val alertTypeId: Short? = null,

    @Schema(description = "ID de la severidad")
    val severityId: Short? = null,

    @Schema(description = "Mensaje descriptivo de la alerta")
    val message: String? = null,

    @Schema(description = "Descripcion detallada de la alerta")
    val description: String? = null
)

package com.apptolast.invernaderos.features.alert.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para resolver una Alerta")
data class AlertResolveRequest(
    @Schema(description = "ID del usuario que resuelve la alerta")
    val resolvedByUserId: Long? = null
)

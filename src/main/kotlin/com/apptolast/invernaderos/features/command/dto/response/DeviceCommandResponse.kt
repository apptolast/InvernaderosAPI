package com.apptolast.invernaderos.features.command.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Respuesta de un comando enviado")
data class DeviceCommandResponse(
    @Schema(description = "Timestamp del envío") val time: Instant,
    @Schema(description = "Código del destino") val code: String,
    @Schema(description = "Valor del comando") val value: String
)

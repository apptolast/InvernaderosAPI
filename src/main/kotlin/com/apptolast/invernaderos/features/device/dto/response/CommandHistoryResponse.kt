package com.apptolast.invernaderos.features.device.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Respuesta que representa un registro de historial de comandos")
data class CommandHistoryResponse(
    @Schema(description = "ID del registro") val id: Long,
    @Schema(description = "Código del registro") val code: String,
    @Schema(description = "ID del dispositivo") val deviceId: Long,
    @Schema(description = "Nombre del comando") val command: String,
    @Schema(description = "Valor del comando") val value: Double?,
    @Schema(description = "Origen del comando") val source: String?,
    @Schema(description = "ID del usuario que envió") val userId: Long?,
    @Schema(description = "Si fue exitoso") val success: Boolean?,
    @Schema(description = "Respuesta del dispositivo") val response: String?,
    @Schema(description = "Fecha de creación") val createdAt: Instant
)

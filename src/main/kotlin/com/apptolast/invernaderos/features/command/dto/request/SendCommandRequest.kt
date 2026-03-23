package com.apptolast.invernaderos.features.command.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para enviar un comando al PLC")
data class SendCommandRequest(
    @Schema(description = "Código del device/setting destino", example = "SET-00036")
    val code: String,

    @Schema(description = "Valor del comando", example = "22")
    val value: String
)

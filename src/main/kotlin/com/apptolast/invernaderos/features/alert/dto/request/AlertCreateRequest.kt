package com.apptolast.invernaderos.features.alert.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para crear una nueva Alerta")
data class AlertCreateRequest(
    @Schema(description = "ID del sector donde ocurre la alerta", required = true)
    val sectorId: Long,

    @Schema(description = "ID del tipo de alerta (1=THRESHOLD_EXCEEDED, 2=SENSOR_OFFLINE, etc.)", example = "1")
    val alertTypeId: Short? = null,

    @Schema(description = "ID de la severidad (1=INFO, 2=WARNING, 3=ERROR, 4=CRITICAL)", example = "2")
    val severityId: Short? = null,

    @Schema(description = "Mensaje descriptivo de la alerta", example = "Temperatura excede el umbral maximo")
    val message: String? = null,

    @Schema(description = "Descripcion detallada de la alerta", example = "Se detecto que la temperatura supero los 35 grados durante mas de 10 minutos")
    val description: String? = null,

    @Schema(description = "Nombre visible para el usuario en el frontend")
    val clientName: String? = null
)

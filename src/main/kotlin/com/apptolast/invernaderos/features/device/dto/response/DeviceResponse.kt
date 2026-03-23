package com.apptolast.invernaderos.features.device.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Respuesta que representa un Dispositivo (Sensor o Actuador)")
data class DeviceResponse(
    @Schema(description = "ID único del dispositivo") val id: Long,
    @Schema(description = "Código único por tenant del dispositivo", example = "DEV-00001") val code: String,
    @Schema(description = "ID del tenant propietario") val tenantId: Long,
    @Schema(description = "ID del sector al que pertenece") val sectorId: Long,
    @Schema(description = "Código del sector") val sectorCode: String?,
    @Schema(description = "Nombre legible del dispositivo") val name: String?,
    @Schema(description = "ID de la categoría (1=SENSOR, 2=ACTUATOR)") val categoryId: Short?,
    @Schema(description = "Nombre de la categoría") val categoryName: String?,
    @Schema(description = "ID del tipo de dispositivo") val typeId: Short?,
    @Schema(description = "Nombre del tipo de dispositivo") val typeName: String?,
    @Schema(description = "ID de la unidad de medida") val unitId: Short?,
    @Schema(description = "Símbolo de la unidad") val unitSymbol: String?,
    @Schema(description = "Si el dispositivo está activo") val isActive: Boolean,
    @Schema(description = "Fecha de creación") val createdAt: Instant,
    @Schema(description = "Fecha de última actualización") val updatedAt: Instant
)

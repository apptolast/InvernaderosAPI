package com.apptolast.invernaderos.features.device.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para actualizar un Dispositivo existente")
data class DeviceUpdateRequest(
    @Schema(description = "ID del sector al que pertenece (debe pertenecer al mismo tenant)")
    val sectorId: Long? = null,

    @Schema(description = "Nombre legible del dispositivo (máx 100 caracteres)")
    @field:jakarta.validation.constraints.Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    val name: String? = null,

    @Schema(description = "ID de la categoría (1=SENSOR, 2=ACTUATOR)")
    val categoryId: Short? = null,

    @Schema(description = "ID del tipo de dispositivo")
    val typeId: Short? = null,

    @Schema(description = "ID de la unidad de medida")
    val unitId: Short? = null,

    @Schema(description = "Si el dispositivo está activo")
    val isActive: Boolean? = null
)

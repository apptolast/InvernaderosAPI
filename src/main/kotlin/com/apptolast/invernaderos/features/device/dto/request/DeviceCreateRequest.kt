package com.apptolast.invernaderos.features.device.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para crear un nuevo Dispositivo")
data class DeviceCreateRequest(
    @Schema(description = "ID del sector donde se instalará el dispositivo", required = true)
    val sectorId: Long,

    @Schema(description = "Nombre legible del dispositivo (máx 100 caracteres)", example = "Sensor Temperatura Invernadero 1")
    @field:jakarta.validation.constraints.Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    val name: String? = null,

    @Schema(description = "ID de la categoría (1=SENSOR, 2=ACTUATOR)", example = "1")
    val categoryId: Short? = null,

    @Schema(description = "ID del tipo de dispositivo (ej: 1=TEMPERATURE, 2=HUMIDITY)", example = "1")
    val typeId: Short? = null,

    @Schema(description = "ID de la unidad de medida", example = "1")
    val unitId: Short? = null,

    @Schema(description = "Nombre visible para el usuario en el frontend")
    val clientName: String? = null,

    @Schema(description = "Si el dispositivo está activo", example = "true")
    val isActive: Boolean? = true
)

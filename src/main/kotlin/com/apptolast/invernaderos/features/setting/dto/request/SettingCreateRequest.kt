package com.apptolast.invernaderos.features.setting.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para crear una nueva configuracion de parametros")
data class SettingCreateRequest(
    @Schema(description = "ID del sector donde aplicar la configuracion", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "El ID del sector es obligatorio")
    val sectorId: Long,

    @Schema(description = "ID del tipo de parametro (device_type)", example = "1", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "El ID del parametro es obligatorio")
    @field:jakarta.validation.constraints.Positive(message = "El ID del parametro debe ser positivo")
    val parameterId: Short,

    @Schema(description = "ID del estado del actuador (1=OFF, 2=ON, 3=AUTO, etc.)", example = "2")
    @field:jakarta.validation.constraints.Positive(message = "El ID del estado del actuador debe ser positivo")
    val actuatorStateId: Short? = null,

    @Schema(description = "ID del tipo de dato (1=INTEGER, 2=LONG, 3=DOUBLE, 4=BOOLEAN, 5=STRING, etc.)", example = "1")
    @field:jakarta.validation.constraints.Positive(message = "El ID del tipo de dato debe ser positivo")
    val dataTypeId: Short? = null,

    @Schema(description = "Valor de la configuracion (se validara segun el tipo de dato)", example = "25")
    val value: String? = null,

    @Schema(description = "Descripcion de la configuracion", example = "Temperatura maxima permitida en el sector")
    val description: String? = null,

    @Schema(description = "Nombre visible para el usuario en el frontend")
    val clientName: String? = null,

    @Schema(description = "Si la configuracion esta activa", example = "true")
    val isActive: Boolean = true
)

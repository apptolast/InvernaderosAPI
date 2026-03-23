package com.apptolast.invernaderos.features.setting.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para actualizar una configuracion existente")
data class SettingUpdateRequest(
    @Schema(description = "ID del sector donde aplicar la configuracion (debe pertenecer al mismo tenant)")
    @field:jakarta.validation.constraints.Positive(message = "El ID del sector debe ser positivo")
    val sectorId: Long? = null,

    @Schema(description = "ID del tipo de parametro (device_type)")
    @field:jakarta.validation.constraints.Positive(message = "El ID del parametro debe ser positivo")
    val parameterId: Short? = null,

    @Schema(description = "ID del estado del actuador")
    @field:jakarta.validation.constraints.Positive(message = "El ID del estado del actuador debe ser positivo")
    val actuatorStateId: Short? = null,

    @Schema(description = "ID del tipo de dato")
    @field:jakarta.validation.constraints.Positive(message = "El ID del tipo de dato debe ser positivo")
    val dataTypeId: Short? = null,

    @Schema(description = "Valor de la configuracion", example = "30")
    val value: String? = null,

    @Schema(description = "Descripcion de la configuracion")
    val description: String? = null,

    @Schema(description = "Nombre visible para el usuario en el frontend")
    val clientName: String? = null,

    @Schema(description = "Si la configuracion esta activa")
    val isActive: Boolean? = null
)

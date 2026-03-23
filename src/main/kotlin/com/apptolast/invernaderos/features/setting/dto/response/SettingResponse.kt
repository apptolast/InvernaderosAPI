package com.apptolast.invernaderos.features.setting.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Respuesta que representa una configuracion de parametros para un sector")
data class SettingResponse(
    @Schema(description = "ID unico de la configuracion")
    val id: Long,

    @Schema(description = "Codigo unico legible de la configuracion", example = "SET-00001")
    val code: String,

    @Schema(description = "ID del sector")
    val sectorId: Long,

    @Schema(description = "ID del tenant propietario")
    val tenantId: Long,

    @Schema(description = "ID del tipo de parametro (device_type)")
    val parameterId: Short,

    @Schema(description = "Nombre del tipo de parametro", example = "TEMPERATURE")
    val parameterName: String?,

    @Schema(description = "ID del estado del actuador")
    val actuatorStateId: Short?,

    @Schema(description = "Nombre del estado del actuador", example = "ON")
    val actuatorStateName: String?,

    @Schema(description = "ID del tipo de dato")
    val dataTypeId: Short?,

    @Schema(description = "Nombre del tipo de dato", example = "INTEGER")
    val dataTypeName: String?,

    @Schema(description = "Valor de la configuracion (String que representa el valor segun el tipo)", example = "25")
    val value: String?,

    @Schema(description = "Descripcion de la configuracion")
    val description: String?,

    @Schema(description = "Si la configuracion esta activa")
    val isActive: Boolean,

    @Schema(description = "Fecha de creacion")
    val createdAt: Instant,

    @Schema(description = "Fecha de ultima actualizacion")
    val updatedAt: Instant
)

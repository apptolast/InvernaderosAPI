package com.apptolast.invernaderos.features.setting.dto

import com.apptolast.invernaderos.features.setting.Setting
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Respuesta que representa una configuracion de parametros para un invernadero")
data class SettingResponse(
    @Schema(description = "ID unico de la configuracion")
    val id: Long,

    @Schema(description = "Codigo unico legible de la configuracion", example = "SET-00001")
    val code: String,

    @Schema(description = "ID del invernadero")
    val greenhouseId: Long,

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

    @Schema(description = "Si la configuracion esta activa")
    val isActive: Boolean,

    @Schema(description = "Fecha de creacion")
    val createdAt: Instant,

    @Schema(description = "Fecha de ultima actualizacion")
    val updatedAt: Instant
)

@Schema(description = "Solicitud para crear una nueva configuracion de parametros")
data class SettingCreateRequest(
    @Schema(description = "ID del invernadero donde aplicar la configuracion", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "El ID del invernadero es obligatorio")
    val greenhouseId: Long,

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

    @Schema(description = "Si la configuracion esta activa", example = "true")
    val isActive: Boolean = true
)

@Schema(description = "Solicitud para actualizar una configuracion existente")
data class SettingUpdateRequest(
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

    @Schema(description = "Si la configuracion esta activa")
    val isActive: Boolean? = null
)

fun Setting.toResponse() = SettingResponse(
    id = this.id ?: throw IllegalStateException("Setting ID cannot be null"),
    code = this.code,
    greenhouseId = this.greenhouseId,
    tenantId = this.tenantId,
    parameterId = this.parameterId,
    parameterName = this.parameter?.name,
    actuatorStateId = this.actuatorStateId,
    actuatorStateName = this.actuatorState?.name,
    dataTypeId = this.dataTypeId,
    dataTypeName = this.dataType?.name,
    value = this.value,
    isActive = this.isActive,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

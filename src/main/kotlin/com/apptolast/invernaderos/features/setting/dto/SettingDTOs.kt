package com.apptolast.invernaderos.features.setting.dto

import com.apptolast.invernaderos.features.setting.Setting
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Schema(description = "Respuesta que representa una configuración de parámetros para un invernadero")
data class SettingResponse(
    @Schema(description = "ID único de la configuración")
    val id: UUID,

    @Schema(description = "ID del invernadero")
    val greenhouseId: UUID,

    @Schema(description = "ID del tenant propietario")
    val tenantId: UUID,

    @Schema(description = "ID del tipo de parámetro (device_type)")
    val parameterId: Short,

    @Schema(description = "Nombre del tipo de parámetro", example = "TEMPERATURE")
    val parameterName: String?,

    @Schema(description = "ID del periodo (1=DAY, 2=NIGHT, 3=ALL)")
    val periodId: Short,

    @Schema(description = "Nombre del periodo", example = "DAY")
    val periodName: String?,

    @Schema(description = "Valor mínimo del rango", example = "15.00")
    val minValue: BigDecimal?,

    @Schema(description = "Valor máximo del rango", example = "30.00")
    val maxValue: BigDecimal?,

    @Schema(description = "Si la configuración está activa")
    val isActive: Boolean,

    @Schema(description = "Fecha de creación")
    val createdAt: Instant,

    @Schema(description = "Fecha de última actualización")
    val updatedAt: Instant
)

@Schema(description = "Solicitud para crear una nueva configuración de parámetros")
data class SettingCreateRequest(
    @Schema(description = "ID del invernadero donde aplicar la configuración", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "El ID del invernadero es obligatorio")
    val greenhouseId: UUID,

    @Schema(description = "ID del tipo de parámetro (device_type)", example = "1", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "El ID del parámetro es obligatorio")
    @field:jakarta.validation.constraints.Positive(message = "El ID del parámetro debe ser positivo")
    val parameterId: Short,

    @Schema(description = "ID del periodo (1=DAY, 2=NIGHT, 3=ALL)", example = "1", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "El ID del periodo es obligatorio")
    @field:jakarta.validation.constraints.Positive(message = "El ID del periodo debe ser positivo")
    val periodId: Short,

    @Schema(description = "Valor mínimo del rango", example = "15.00")
    val minValue: BigDecimal? = null,

    @Schema(description = "Valor máximo del rango", example = "30.00")
    val maxValue: BigDecimal? = null,

    @Schema(description = "Si la configuración está activa", example = "true")
    val isActive: Boolean = true
)

@Schema(description = "Solicitud para actualizar una configuración existente")
data class SettingUpdateRequest(
    @Schema(description = "ID del tipo de parámetro (device_type)")
    @field:jakarta.validation.constraints.Positive(message = "El ID del parámetro debe ser positivo")
    val parameterId: Short? = null,

    @Schema(description = "ID del periodo (1=DAY, 2=NIGHT, 3=ALL)")
    @field:jakarta.validation.constraints.Positive(message = "El ID del periodo debe ser positivo")
    val periodId: Short? = null,

    @Schema(description = "Valor mínimo del rango", example = "15.00")
    val minValue: BigDecimal? = null,

    @Schema(description = "Valor máximo del rango", example = "30.00")
    val maxValue: BigDecimal? = null,

    @Schema(description = "Si la configuración está activa")
    val isActive: Boolean? = null
)

fun Setting.toResponse() = SettingResponse(
    id = this.id ?: throw IllegalStateException("Setting ID cannot be null"),
    greenhouseId = this.greenhouseId,
    tenantId = this.tenantId,
    parameterId = this.parameterId,
    parameterName = this.parameter?.name,
    periodId = this.periodId,
    periodName = this.period?.name,
    minValue = this.minValue,
    maxValue = this.maxValue,
    isActive = this.isActive,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

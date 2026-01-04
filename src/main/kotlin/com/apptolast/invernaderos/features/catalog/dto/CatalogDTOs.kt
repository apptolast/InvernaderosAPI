package com.apptolast.invernaderos.features.catalog.dto

import com.apptolast.invernaderos.features.catalog.DeviceCategory
import com.apptolast.invernaderos.features.catalog.DeviceType
import com.apptolast.invernaderos.features.catalog.Unit
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Categoría de dispositivo (SENSOR o ACTUATOR)")
data class DeviceCategoryResponse(
    @Schema(description = "ID de la categoría", example = "1")
    val id: Short,

    @Schema(description = "Nombre de la categoría", example = "SENSOR")
    val name: String
)

@Schema(description = "Tipo de dispositivo con información completa")
data class DeviceTypeResponse(
    @Schema(description = "ID del tipo", example = "1")
    val id: Short,

    @Schema(description = "Nombre del tipo", example = "TEMPERATURE")
    val name: String,

    @Schema(description = "Descripción del tipo", example = "Sensor de temperatura ambiente")
    val description: String?,

    @Schema(description = "ID de la categoría a la que pertenece (1=SENSOR, 2=ACTUATOR)", example = "1")
    val categoryId: Short,

    @Schema(description = "Nombre de la categoría", example = "SENSOR")
    val categoryName: String?,

    @Schema(description = "ID de la unidad por defecto", example = "1")
    val defaultUnitId: Short?,

    @Schema(description = "Símbolo de la unidad por defecto", example = "°C")
    val defaultUnitSymbol: String?,

    @Schema(description = "Tipo de dato que genera", example = "DECIMAL")
    val dataType: String?,

    @Schema(description = "Valor mínimo esperado físicamente", example = "-50.00")
    val minExpectedValue: BigDecimal?,

    @Schema(description = "Valor máximo esperado físicamente", example = "100.00")
    val maxExpectedValue: BigDecimal?,

    @Schema(description = "Tipo de control para actuadores: BINARY, CONTINUOUS, MULTI_STATE", example = "CONTINUOUS")
    val controlType: String?,

    @Schema(description = "Si el tipo está activo", example = "true")
    val isActive: Boolean
)

@Schema(description = "Unidad de medida")
data class UnitResponse(
    @Schema(description = "ID de la unidad", example = "1")
    val id: Short,

    @Schema(description = "Símbolo de la unidad", example = "°C")
    val symbol: String,

    @Schema(description = "Nombre de la unidad", example = "Grados Celsius")
    val name: String,

    @Schema(description = "Descripción de la unidad", example = "Temperatura en grados Celsius")
    val description: String?,

    @Schema(description = "Si la unidad está activa", example = "true")
    val isActive: Boolean
)

fun DeviceCategory.toResponse() = DeviceCategoryResponse(
    id = this.id,
    name = this.name
)

fun DeviceType.toResponse() = DeviceTypeResponse(
    id = this.id ?: throw IllegalStateException("DeviceType ID cannot be null"),
    name = this.name,
    description = this.description,
    categoryId = this.categoryId,
    categoryName = this.category?.name,
    defaultUnitId = this.defaultUnitId,
    defaultUnitSymbol = this.defaultUnit?.symbol,
    dataType = this.dataType,
    minExpectedValue = this.minExpectedValue,
    maxExpectedValue = this.maxExpectedValue,
    controlType = this.controlType,
    isActive = this.isActive
)

fun Unit.toResponse() = UnitResponse(
    id = this.id ?: throw IllegalStateException("Unit ID cannot be null"),
    symbol = this.symbol,
    name = this.name,
    description = this.description,
    isActive = this.isActive
)

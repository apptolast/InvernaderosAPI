package com.apptolast.invernaderos.features.catalog.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

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

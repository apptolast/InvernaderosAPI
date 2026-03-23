package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

/**
 * DTO para actualizar un tipo de dispositivo existente.
 */
@Schema(description = "Request para actualizar un tipo de dispositivo")
data class DeviceTypeUpdateRequest(
    @Schema(description = "Nuevo nombre del tipo (máx 30 caracteres)", example = "NITROGEN_SENSOR_V2")
    @field:jakarta.validation.constraints.Size(max = 30, message = "El nombre no puede exceder 30 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripción del tipo", example = "Sensor de nitrógeno mejorado")
    val description: String? = null,

    @Schema(description = "Nueva categoría (1=SENSOR, 2=ACTUATOR)", example = "1")
    val categoryId: Short? = null,

    @Schema(description = "Nuevo ID de unidad por defecto", example = "6")
    val defaultUnitId: Short? = null,

    @Schema(description = "Nuevo tipo de dato: DECIMAL, INTEGER, BOOLEAN, TEXT, JSON", example = "DECIMAL")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^(DECIMAL|INTEGER|BOOLEAN|TEXT|JSON)$",
        message = "dataType debe ser: DECIMAL, INTEGER, BOOLEAN, TEXT o JSON"
    )
    val dataType: String? = null,

    @Schema(description = "Nuevo valor mínimo esperado", example = "0.00")
    val minExpectedValue: BigDecimal? = null,

    @Schema(description = "Nuevo valor máximo esperado", example = "150.00")
    val maxExpectedValue: BigDecimal? = null,

    @Schema(description = "Nuevo tipo de control: BINARY, CONTINUOUS, MULTI_STATE", example = "BINARY")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^(BINARY|CONTINUOUS|MULTI_STATE)$",
        message = "controlType debe ser: BINARY, CONTINUOUS o MULTI_STATE"
    )
    val controlType: String? = null,

    @Schema(description = "Nuevo estado activo/inactivo", example = "true")
    val isActive: Boolean? = null
)

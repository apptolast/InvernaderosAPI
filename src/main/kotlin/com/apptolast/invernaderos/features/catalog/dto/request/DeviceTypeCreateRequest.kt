package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

/**
 * DTO para crear un nuevo tipo de dispositivo.
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un tipo de dispositivo")
data class DeviceTypeCreateRequest(
    @Schema(description = "Nombre único del tipo (máx 30 caracteres)", example = "NITROGEN_SENSOR", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 30, message = "El nombre no puede exceder 30 caracteres")
    val name: String,

    @Schema(description = "Descripción del tipo de dispositivo", example = "Sensor de nitrógeno en suelo")
    val description: String? = null,

    @Schema(description = "ID de la categoría (1=SENSOR, 2=ACTUATOR)", example = "1", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "La categoría es obligatoria")
    val categoryId: Short,

    @Schema(description = "ID de la unidad por defecto", example = "5")
    val defaultUnitId: Short? = null,

    @Schema(description = "Tipo de dato: DECIMAL, INTEGER, BOOLEAN, TEXT, JSON", example = "DECIMAL")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^(DECIMAL|INTEGER|BOOLEAN|TEXT|JSON)$",
        message = "dataType debe ser: DECIMAL, INTEGER, BOOLEAN, TEXT o JSON"
    )
    val dataType: String? = "DECIMAL",

    @Schema(description = "Valor mínimo esperado físicamente", example = "0.00")
    val minExpectedValue: BigDecimal? = null,

    @Schema(description = "Valor máximo esperado físicamente", example = "100.00")
    val maxExpectedValue: BigDecimal? = null,

    @Schema(description = "Tipo de control para actuadores: BINARY, CONTINUOUS, MULTI_STATE", example = "CONTINUOUS")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^(BINARY|CONTINUOUS|MULTI_STATE)$",
        message = "controlType debe ser: BINARY, CONTINUOUS o MULTI_STATE"
    )
    val controlType: String? = null,

    @Schema(description = "Si el tipo está activo", example = "true")
    val isActive: Boolean = true
)

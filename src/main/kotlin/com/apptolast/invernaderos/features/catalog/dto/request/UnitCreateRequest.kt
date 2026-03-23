package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para crear una nueva unidad de medida.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear una unidad de medida")
data class UnitCreateRequest(
    @Schema(description = "Símbolo de la unidad (único, máx 10 caracteres)", example = "mg/L", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El símbolo es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 10, message = "El símbolo no puede exceder 10 caracteres")
    val symbol: String,

    @Schema(description = "Nombre de la unidad (máx 50 caracteres)", example = "Miligramos por litro", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    val name: String,

    @Schema(description = "Descripción de la unidad", example = "Concentración de nutrientes")
    val description: String? = null,

    @Schema(description = "Si la unidad está activa", example = "true")
    val isActive: Boolean = true
)

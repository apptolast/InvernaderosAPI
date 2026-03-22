package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para crear un nuevo estado de actuador.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un estado de actuador")
data class ActuatorStateCreateRequest(
    @Schema(description = "Nombre del estado (único, máx 20 caracteres)", example = "WARMING_UP", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String,

    @Schema(description = "Descripción del estado", example = "Calentando el sistema")
    val description: String? = null,

    @Schema(description = "Si el actuador está funcionando en este estado", example = "true")
    val isOperational: Boolean = false,

    @Schema(description = "Orden para mostrar en UI (mayor = más abajo)", example = "5")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El orden no puede ser negativo")
    val displayOrder: Short = 0,

    @Schema(description = "Color hexadecimal para UI (7 caracteres con #)", example = "#FFA500")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color debe ser un código hexadecimal válido (ej: #FF0000)"
    )
    val color: String? = null
)

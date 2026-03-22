package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para crear un nuevo tipo de alerta.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un tipo de alerta")
data class AlertTypeCreateRequest(
    @Schema(description = "Nombre único del tipo de alerta (máx 30 caracteres)", example = "MAINTENANCE_REQUIRED", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 30, message = "El nombre no puede exceder 30 caracteres")
    val name: String,

    @Schema(description = "Descripción del tipo de alerta", example = "Se requiere mantenimiento preventivo")
    val description: String? = null
)

package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para crear un nuevo nivel de severidad de alerta.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un nivel de severidad de alerta")
data class AlertSeverityCreateRequest(
    @Schema(description = "Nombre único de la severidad (máx 20 caracteres)", example = "URGENT", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String,

    @Schema(description = "Nivel numérico para ordenación (1=más bajo, mayor=más alto)", example = "5", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "El nivel es obligatorio")
    @field:jakarta.validation.constraints.Positive(message = "El nivel debe ser positivo")
    val level: Short,

    @Schema(description = "Descripción de la severidad", example = "Requiere atención urgente")
    val description: String? = null,

    @Schema(description = "Color hexadecimal para UI (7 caracteres con #)", example = "#FF00FF")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color debe ser un código hexadecimal válido (ej: #FF0000)"
    )
    val color: String? = null,

    @Schema(description = "Si requiere acción inmediata", example = "true")
    val requiresAction: Boolean = false,

    @Schema(description = "Minutos de retraso antes de notificar", example = "0")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El retraso no puede ser negativo")
    val notificationDelayMinutes: Int = 0
)

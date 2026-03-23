package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para actualizar un nivel de severidad existente.
 */
@Schema(description = "Request para actualizar un nivel de severidad de alerta")
data class AlertSeverityUpdateRequest(
    @Schema(description = "Nuevo nombre de la severidad (máx 20 caracteres)", example = "URGENT_V2")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String? = null,

    @Schema(description = "Nuevo nivel numérico para ordenación", example = "5")
    @field:jakarta.validation.constraints.Positive(message = "El nivel debe ser positivo")
    val level: Short? = null,

    @Schema(description = "Nueva descripción de la severidad", example = "Requiere atención urgente inmediata")
    val description: String? = null,

    @Schema(description = "Nuevo color hexadecimal para UI", example = "#FF00FF")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color debe ser un código hexadecimal válido (ej: #FF0000)"
    )
    val color: String? = null,

    @Schema(description = "Si requiere acción inmediata", example = "true")
    val requiresAction: Boolean? = null,

    @Schema(description = "Minutos de retraso antes de notificar", example = "5")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El retraso no puede ser negativo")
    val notificationDelayMinutes: Int? = null
)

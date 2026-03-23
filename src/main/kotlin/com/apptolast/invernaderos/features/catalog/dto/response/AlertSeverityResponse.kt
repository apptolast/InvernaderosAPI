package com.apptolast.invernaderos.features.catalog.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Nivel de severidad de alerta")
data class AlertSeverityResponse(
    @Schema(description = "ID de la severidad", example = "1")
    val id: Short,

    @Schema(description = "Nombre de la severidad", example = "INFO")
    val name: String,

    @Schema(description = "Nivel numérico para ordenación (1=bajo, 4=crítico)", example = "1")
    val level: Short,

    @Schema(description = "Descripción de la severidad")
    val description: String?,

    @Schema(description = "Color hexadecimal para UI", example = "#0066FF")
    val color: String?,

    @Schema(description = "Si requiere acción inmediata", example = "false")
    val requiresAction: Boolean,

    @Schema(description = "Minutos de retraso antes de notificar", example = "0")
    val notificationDelayMinutes: Int
)

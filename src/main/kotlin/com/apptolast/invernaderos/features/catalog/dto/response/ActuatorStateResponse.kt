package com.apptolast.invernaderos.features.catalog.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Estado de actuador")
data class ActuatorStateResponse(
    @Schema(description = "ID del estado", example = "1")
    val id: Short,

    @Schema(description = "Nombre del estado", example = "ON")
    val name: String,

    @Schema(description = "Descripción del estado", example = "Encendido")
    val description: String?,

    @Schema(description = "Si el actuador está funcionando en este estado", example = "true")
    val isOperational: Boolean,

    @Schema(description = "Orden para mostrar en UI", example = "1")
    val displayOrder: Short,

    @Schema(description = "Color hexadecimal para UI", example = "#00FF00")
    val color: String?
)

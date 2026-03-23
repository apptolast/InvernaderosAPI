package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Respuesta que representa una Alerta del sistema")
data class AlertResponse(
    @Schema(description = "ID unico de la alerta") val id: Long,
    @Schema(description = "Codigo unico legible de la alerta", example = "ALT-00001") val code: String,
    @Schema(description = "ID del tenant propietario") val tenantId: Long,
    @Schema(description = "ID del sector donde ocurrio la alerta") val sectorId: Long,
    @Schema(description = "Codigo del sector") val sectorCode: String?,
    @Schema(description = "ID del tipo de alerta") val alertTypeId: Short?,
    @Schema(description = "Nombre del tipo de alerta") val alertTypeName: String?,
    @Schema(description = "ID de la severidad") val severityId: Short?,
    @Schema(description = "Nombre de la severidad (INFO, WARNING, ERROR, CRITICAL)") val severityName: String?,
    @Schema(description = "Nivel de severidad (1-4)") val severityLevel: Short?,
    @Schema(description = "Mensaje descriptivo de la alerta") val message: String?,
    @Schema(description = "Descripcion detallada de la alerta") val description: String?,
    @Schema(description = "Indica si la alerta esta resuelta") val isResolved: Boolean,
    @Schema(description = "Fecha/hora de resolucion") val resolvedAt: Instant?,
    @Schema(description = "ID del usuario que resolvio la alerta") val resolvedByUserId: Long?,
    @Schema(description = "Nombre del usuario que resolvio") val resolvedByUserName: String?,
    @Schema(description = "Fecha de creacion") val createdAt: Instant,
    @Schema(description = "Fecha de ultima actualizacion") val updatedAt: Instant
)

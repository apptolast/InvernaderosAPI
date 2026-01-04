package com.apptolast.invernaderos.features.alert.dto

import com.apptolast.invernaderos.features.alert.Alert
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Respuesta que representa una Alerta del sistema")
data class AlertResponse(
    @Schema(description = "ID único de la alerta") val id: UUID,
    @Schema(description = "ID del tenant propietario") val tenantId: UUID,
    @Schema(description = "ID del invernadero donde ocurrió la alerta") val greenhouseId: UUID,
    @Schema(description = "Nombre del invernadero") val greenhouseName: String?,
    @Schema(description = "ID del tipo de alerta") val alertTypeId: Short?,
    @Schema(description = "Nombre del tipo de alerta") val alertTypeName: String?,
    @Schema(description = "ID de la severidad") val severityId: Short?,
    @Schema(description = "Nombre de la severidad (INFO, WARNING, ERROR, CRITICAL)") val severityName: String?,
    @Schema(description = "Nivel de severidad (1-4)") val severityLevel: Short?,
    @Schema(description = "Mensaje descriptivo de la alerta") val message: String,
    @Schema(description = "Indica si la alerta está resuelta") val isResolved: Boolean,
    @Schema(description = "Fecha/hora de resolución") val resolvedAt: Instant?,
    @Schema(description = "ID del usuario que resolvió la alerta") val resolvedByUserId: UUID?,
    @Schema(description = "Nombre del usuario que resolvió") val resolvedByUserName: String?,
    @Schema(description = "Fecha de creación") val createdAt: Instant,
    @Schema(description = "Fecha de última actualización") val updatedAt: Instant
)

@Schema(description = "Solicitud para crear una nueva Alerta")
data class AlertCreateRequest(
    @Schema(description = "ID del invernadero donde ocurre la alerta", required = true)
    val greenhouseId: UUID,

    @Schema(description = "ID del tipo de alerta (1=THRESHOLD_EXCEEDED, 2=SENSOR_OFFLINE, etc.)", example = "1")
    val alertTypeId: Short? = null,

    @Schema(description = "ID de la severidad (1=INFO, 2=WARNING, 3=ERROR, 4=CRITICAL)", example = "2")
    val severityId: Short? = null,

    @Schema(description = "Mensaje descriptivo de la alerta", required = true, example = "Temperatura excede el umbral máximo")
    val message: String
)

@Schema(description = "Solicitud para actualizar una Alerta existente")
data class AlertUpdateRequest(
    @Schema(description = "ID del tipo de alerta")
    val alertTypeId: Short? = null,

    @Schema(description = "ID de la severidad")
    val severityId: Short? = null,

    @Schema(description = "Mensaje descriptivo de la alerta")
    val message: String? = null
)

@Schema(description = "Solicitud para resolver una Alerta")
data class AlertResolveRequest(
    @Schema(description = "ID del usuario que resuelve la alerta")
    val resolvedByUserId: UUID? = null
)

fun Alert.toResponse() = AlertResponse(
    id = this.id,
    tenantId = this.tenantId,
    greenhouseId = this.greenhouseId,
    greenhouseName = this.greenhouse?.name,
    alertTypeId = this.alertTypeId,
    alertTypeName = this.alertType?.name,
    severityId = this.severityId,
    severityName = this.severity?.name,
    severityLevel = this.severity?.level,
    message = this.message,
    isResolved = this.isResolved,
    resolvedAt = this.resolvedAt,
    resolvedByUserId = this.resolvedByUserId,
    resolvedByUserName = this.resolvedByUser?.username,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

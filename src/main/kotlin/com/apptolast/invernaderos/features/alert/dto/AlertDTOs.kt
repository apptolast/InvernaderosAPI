package com.apptolast.invernaderos.features.alert.dto

import com.apptolast.invernaderos.features.alert.Alert
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Respuesta que representa una Alerta del sistema")
data class AlertResponse(
    @Schema(description = "ID Ãºnico de la alerta") val id: Long,
    @Schema(description = "CÃ³digo Ãºnico legible de la alerta", example = "ALT-00001") val code: String,
    @Schema(description = "ID del tenant propietario") val tenantId: Long,
    @Schema(description = "ID del sector donde ocurriÃ³ la alerta") val sectorId: Long,
    @Schema(description = "CÃ³digo del sector") val sectorCode: String?,
    @Schema(description = "ID del tipo de alerta") val alertTypeId: Short?,
    @Schema(description = "Nombre del tipo de alerta") val alertTypeName: String?,
    @Schema(description = "ID de la severidad") val severityId: Short?,
    @Schema(description = "Nombre de la severidad (INFO, WARNING, ERROR, CRITICAL)") val severityName: String?,
    @Schema(description = "Nivel de severidad (1-4)") val severityLevel: Short?,
    @Schema(description = "Mensaje descriptivo de la alerta") val message: String?,
    @Schema(description = "Descripcion detallada de la alerta") val description: String?,
    @Schema(description = "Indica si la alerta esta resuelta") val isResolved: Boolean,
    @Schema(description = "Fecha/hora de resoluciÃ³n") val resolvedAt: Instant?,
    @Schema(description = "ID del usuario que resolviÃ³ la alerta") val resolvedByUserId: Long?,
    @Schema(description = "Nombre del usuario que resolviÃ³") val resolvedByUserName: String?,
    @Schema(description = "Fecha de creaciÃ³n") val createdAt: Instant,
    @Schema(description = "Fecha de Ãºltima actualizaciÃ³n") val updatedAt: Instant
)

@Schema(description = "Solicitud para crear una nueva Alerta")
data class AlertCreateRequest(
    @Schema(description = "ID del sector donde ocurre la alerta", required = true)
    val sectorId: Long,

    @Schema(description = "ID del tipo de alerta (1=THRESHOLD_EXCEEDED, 2=SENSOR_OFFLINE, etc.)", example = "1")
    val alertTypeId: Short? = null,

    @Schema(description = "ID de la severidad (1=INFO, 2=WARNING, 3=ERROR, 4=CRITICAL)", example = "2")
    val severityId: Short? = null,

    @Schema(description = "Mensaje descriptivo de la alerta", example = "Temperatura excede el umbral maximo")
    val message: String? = null,

    @Schema(description = "Descripcion detallada de la alerta", example = "Se detecto que la temperatura supero los 35 grados durante mas de 10 minutos")
    val description: String? = null
)

@Schema(description = "Solicitud para actualizar una Alerta existente")
data class AlertUpdateRequest(
    @Schema(description = "ID del tipo de alerta")
    val alertTypeId: Short? = null,

    @Schema(description = "ID de la severidad")
    val severityId: Short? = null,

    @Schema(description = "Mensaje descriptivo de la alerta")
    val message: String? = null,

    @Schema(description = "Descripcion detallada de la alerta")
    val description: String? = null
)

@Schema(description = "Solicitud para resolver una Alerta")
data class AlertResolveRequest(
    @Schema(description = "ID del usuario que resuelve la alerta")
    val resolvedByUserId: Long? = null
)

fun Alert.toResponse() = AlertResponse(
    id = this.id ?: throw IllegalStateException("Alert ID cannot be null"),
    code = this.code,
    tenantId = this.tenantId,
    sectorId = this.sectorId,
    sectorCode = this.sector?.code,
    alertTypeId = this.alertTypeId,
    alertTypeName = this.alertType?.name,
    severityId = this.severityId,
    severityName = this.severity?.name,
    severityLevel = this.severity?.level,
    message = this.message,
    description = this.description,
    isResolved = this.isResolved,
    resolvedAt = this.resolvedAt,
    resolvedByUserId = this.resolvedByUserId,
    resolvedByUserName = this.resolvedByUser?.username,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

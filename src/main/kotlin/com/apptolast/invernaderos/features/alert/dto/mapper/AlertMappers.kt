package com.apptolast.invernaderos.features.alert.dto.mapper

import com.apptolast.invernaderos.features.alert.Alert as AlertEntity
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.CreateAlertCommand
import com.apptolast.invernaderos.features.alert.domain.port.input.UpdateAlertCommand
import com.apptolast.invernaderos.features.alert.dto.request.AlertCreateRequest
import com.apptolast.invernaderos.features.alert.dto.request.AlertUpdateRequest
import com.apptolast.invernaderos.features.alert.dto.response.AlertResponse
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

// --- Request → Command ---

fun AlertCreateRequest.toCommand(tenantId: TenantId) = CreateAlertCommand(
    tenantId = tenantId,
    sectorId = SectorId(sectorId),
    alertTypeId = alertTypeId,
    severityId = severityId,
    message = message,
    description = description
)

fun AlertUpdateRequest.toCommand(id: Long, tenantId: TenantId) = UpdateAlertCommand(
    id = id,
    tenantId = tenantId,
    sectorId = sectorId?.let { SectorId(it) },
    alertTypeId = alertTypeId,
    severityId = severityId,
    message = message,
    description = description
)

// --- Domain → Response ---

fun AlertEntity.toResponse() = toDomain().toResponse()

fun Alert.toResponse() = AlertResponse(
    id = id ?: throw IllegalStateException("Alert ID cannot be null"),
    code = code,
    tenantId = tenantId.value,
    sectorId = sectorId.value,
    sectorCode = sectorCode,
    alertTypeId = alertTypeId,
    alertTypeName = alertTypeName,
    severityId = severityId,
    severityName = severityName,
    severityLevel = severityLevel,
    message = message,
    description = description,
    isResolved = isResolved,
    resolvedAt = resolvedAt,
    resolvedByUserId = resolvedByUserId,
    resolvedByUserName = resolvedByUserName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Entity ↔ Domain ---

fun AlertEntity.toDomain() = Alert(
    id = id,
    code = code,
    tenantId = TenantId(tenantId),
    sectorId = SectorId(sectorId),
    sectorCode = sector?.code,
    alertTypeId = alertTypeId,
    alertTypeName = alertType?.name,
    severityId = severityId,
    severityName = severity?.name,
    severityLevel = severity?.level,
    message = message,
    description = description,
    isResolved = isResolved,
    resolvedAt = resolvedAt,
    resolvedByUserId = resolvedByUserId,
    resolvedByUserName = resolvedByUser?.username,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Alert.toEntity() = AlertEntity(
    id = id,
    code = code,
    tenantId = tenantId.value,
    sectorId = sectorId.value,
    alertTypeId = alertTypeId,
    severityId = severityId,
    message = message,
    description = description,
    isResolved = isResolved,
    resolvedAt = resolvedAt,
    resolvedByUserId = resolvedByUserId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

package com.apptolast.invernaderos.features.alert.dto.mapper

import com.apptolast.invernaderos.features.alert.Alert as AlertEntity
import com.apptolast.invernaderos.features.alert.AlertStateChange as AlertStateChangeEntity
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
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
    description = description,
    clientName = clientName
)

fun AlertUpdateRequest.toCommand(id: Long, tenantId: TenantId) = UpdateAlertCommand(
    id = id,
    tenantId = tenantId,
    sectorId = sectorId?.let { SectorId(it) },
    alertTypeId = alertTypeId,
    severityId = severityId,
    message = message,
    description = description,
    clientName = clientName
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
    clientName = clientName,
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
    clientName = clientName,
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
    clientName = clientName,
    isResolved = isResolved,
    resolvedAt = resolvedAt,
    resolvedByUserId = resolvedByUserId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- AlertStateChange Entity ↔ Domain ---

fun AlertStateChangeEntity.toDomain() = AlertStateChange(
    id = id,
    alertId = alertId,
    fromResolved = fromResolved,
    toResolved = toResolved,
    source = AlertSignalSource.valueOf(source),
    rawValue = rawValue,
    at = at,
    actor = when (actorKind) {
        "USER" -> AlertActor.User(
            userId = actorUserId ?: 0L,
            username = null,        // hydrated by read adapter via JOIN; not available here
            displayName = null,
        )
        "DEVICE" -> AlertActor.Device(deviceRef = actorRef)
        else -> AlertActor.System
    }
)

fun AlertStateChange.toEntity(): AlertStateChangeEntity {
    val (actorKind, actorUserId, actorRef) = when (val a = actor) {
        is AlertActor.User -> Triple("USER", a.userId, null)
        is AlertActor.Device -> Triple("DEVICE", null, a.deviceRef)
        AlertActor.System -> Triple("SYSTEM", null, null)
    }
    return AlertStateChangeEntity(
        id = id,
        alertId = alertId,
        fromResolved = fromResolved,
        toResolved = toResolved,
        source = source.name,
        rawValue = rawValue,
        at = at,
        actorKind = actorKind,
        actorUserId = actorUserId,
        actorRef = actorRef,
    )
}

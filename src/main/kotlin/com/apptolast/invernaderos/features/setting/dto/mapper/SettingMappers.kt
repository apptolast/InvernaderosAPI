package com.apptolast.invernaderos.features.setting.dto.mapper

import com.apptolast.invernaderos.features.setting.Setting as SettingEntity
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.input.CreateSettingCommand
import com.apptolast.invernaderos.features.setting.domain.port.input.UpdateSettingCommand
import com.apptolast.invernaderos.features.setting.dto.request.SettingCreateRequest
import com.apptolast.invernaderos.features.setting.dto.request.SettingUpdateRequest
import com.apptolast.invernaderos.features.setting.dto.response.SettingResponse
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

// --- Request → Command ---

fun SettingCreateRequest.toCommand(tenantId: TenantId) = CreateSettingCommand(
    tenantId = tenantId,
    sectorId = SectorId(sectorId),
    parameterId = parameterId,
    actuatorStateId = actuatorStateId,
    dataTypeId = dataTypeId,
    value = value,
    description = description,
    isActive = isActive
)

fun SettingUpdateRequest.toCommand(id: SettingId, tenantId: TenantId) = UpdateSettingCommand(
    id = id,
    tenantId = tenantId,
    sectorId = sectorId?.let { SectorId(it) },
    parameterId = parameterId,
    actuatorStateId = actuatorStateId,
    dataTypeId = dataTypeId,
    value = value,
    description = description,
    isActive = isActive
)

// --- Domain → Response ---

fun Setting.toResponse() = SettingResponse(
    id = id?.value ?: throw IllegalStateException("Setting ID cannot be null"),
    code = code,
    sectorId = sectorId.value,
    tenantId = tenantId.value,
    parameterId = parameterId,
    parameterName = parameterName,
    actuatorStateId = actuatorStateId,
    actuatorStateName = actuatorStateName,
    dataTypeId = dataTypeId,
    dataTypeName = dataTypeName,
    value = value,
    description = description,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Entity ↔ Domain ---

fun SettingEntity.toDomain() = Setting(
    id = id?.let { SettingId(it) },
    code = code,
    tenantId = TenantId(tenantId),
    sectorId = SectorId(sectorId),
    parameterId = parameterId,
    parameterName = parameter?.name,
    actuatorStateId = actuatorStateId,
    actuatorStateName = actuatorState?.name,
    dataTypeId = dataTypeId,
    dataTypeName = dataType?.name,
    value = value,
    description = description,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Setting.toEntity() = SettingEntity(
    id = id?.value,
    code = code,
    sectorId = sectorId.value,
    tenantId = tenantId.value,
    parameterId = parameterId,
    actuatorStateId = actuatorStateId,
    dataTypeId = dataTypeId,
    value = value,
    description = description,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

package com.apptolast.invernaderos.features.device.dto.mapper

import com.apptolast.invernaderos.features.device.Device as DeviceEntity
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.input.CreateDeviceCommand
import com.apptolast.invernaderos.features.device.domain.port.input.UpdateDeviceCommand
import com.apptolast.invernaderos.features.device.dto.request.DeviceCreateRequest
import com.apptolast.invernaderos.features.device.dto.request.DeviceUpdateRequest
import com.apptolast.invernaderos.features.device.dto.response.DeviceResponse
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

// --- Request → Command ---

fun DeviceCreateRequest.toCommand(tenantId: TenantId) = CreateDeviceCommand(
    tenantId = tenantId,
    sectorId = SectorId(sectorId),
    name = name,
    categoryId = categoryId,
    typeId = typeId,
    unitId = unitId,
    clientName = clientName,
    isActive = isActive ?: true
)

fun DeviceUpdateRequest.toCommand(id: DeviceId, tenantId: TenantId) = UpdateDeviceCommand(
    id = id,
    tenantId = tenantId,
    sectorId = sectorId?.let { SectorId(it) },
    name = name,
    categoryId = categoryId,
    typeId = typeId,
    unitId = unitId,
    clientName = clientName,
    isActive = isActive
)

// --- Domain → Response ---

fun Device.toResponse() = DeviceResponse(
    id = id?.value ?: throw IllegalStateException("Device ID cannot be null"),
    code = code,
    tenantId = tenantId.value,
    sectorId = sectorId.value,
    sectorCode = sectorCode,
    name = name,
    categoryId = categoryId,
    categoryName = categoryName,
    typeId = typeId,
    typeName = typeName,
    unitId = unitId,
    unitSymbol = unitSymbol,
    clientName = clientName,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Entity ↔ Domain ---

fun DeviceEntity.toDomain() = Device(
    id = id?.let { DeviceId(it) },
    code = code,
    tenantId = TenantId(tenantId),
    sectorId = SectorId(sectorId),
    sectorCode = sector?.code,
    name = name,
    categoryId = categoryId,
    categoryName = category?.name,
    typeId = typeId,
    typeName = type?.name,
    unitId = unitId,
    unitSymbol = unit?.symbol,
    clientName = clientName,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Device.toEntity() = DeviceEntity(
    id = id?.value,
    code = code,
    tenantId = tenantId.value,
    sectorId = sectorId.value,
    name = name,
    categoryId = categoryId,
    typeId = typeId,
    unitId = unitId,
    clientName = clientName,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

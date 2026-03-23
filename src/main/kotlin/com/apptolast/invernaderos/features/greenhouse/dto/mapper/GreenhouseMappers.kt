package com.apptolast.invernaderos.features.greenhouse.dto.mapper

import com.apptolast.invernaderos.features.greenhouse.Greenhouse as GreenhouseEntity
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.CreateGreenhouseCommand
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.UpdateGreenhouseCommand
import com.apptolast.invernaderos.features.greenhouse.dto.request.GreenhouseCreateRequest
import com.apptolast.invernaderos.features.greenhouse.dto.request.GreenhouseUpdateRequest
import com.apptolast.invernaderos.features.greenhouse.dto.response.GreenhouseResponse
import com.apptolast.invernaderos.features.shared.domain.model.Location
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.LocationDto

// --- Request → Command ---

fun GreenhouseCreateRequest.toCommand(tenantId: TenantId) = CreateGreenhouseCommand(
    tenantId = tenantId,
    name = name,
    location = location?.toDomain(),
    areaM2 = areaM2,
    timezone = timezone,
    isActive = isActive ?: true
)

fun GreenhouseUpdateRequest.toCommand(id: GreenhouseId, tenantId: TenantId) = UpdateGreenhouseCommand(
    id = id,
    tenantId = tenantId,
    name = name,
    location = location?.toDomain(),
    areaM2 = areaM2,
    timezone = timezone,
    isActive = isActive
)

// --- Domain → Response ---

fun Greenhouse.toResponse() = GreenhouseResponse(
    id = id?.value ?: throw IllegalStateException("Greenhouse ID cannot be null"),
    code = code,
    name = name,
    tenantId = tenantId.value,
    location = location?.toLocationDto(),
    areaM2 = areaM2,
    timezone = timezone,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Entity ↔ Domain ---

fun GreenhouseEntity.toDomain() = Greenhouse(
    id = id?.let { GreenhouseId(it) },
    code = code,
    tenantId = TenantId(tenantId),
    name = name,
    location = location?.toDomain(),
    areaM2 = areaM2,
    timezone = timezone,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Greenhouse.toEntity() = GreenhouseEntity(
    id = id?.value,
    code = code,
    tenantId = tenantId.value,
    name = name,
    location = location?.toLocationDto(),
    areaM2 = areaM2,
    timezone = timezone,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- LocationDto ↔ Location ---

fun LocationDto.toDomain() = Location(
    lat = lat,
    lon = lon
)

fun Location.toLocationDto() = LocationDto(
    lat = lat,
    lon = lon
)

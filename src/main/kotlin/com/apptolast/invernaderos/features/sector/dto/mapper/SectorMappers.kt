package com.apptolast.invernaderos.features.sector.dto.mapper

import com.apptolast.invernaderos.features.sector.Sector as SectorEntity
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.sector.domain.port.input.CreateSectorCommand
import com.apptolast.invernaderos.features.sector.domain.port.input.UpdateSectorCommand
import com.apptolast.invernaderos.features.sector.dto.request.SectorCreateRequest
import com.apptolast.invernaderos.features.sector.dto.request.SectorUpdateRequest
import com.apptolast.invernaderos.features.sector.dto.response.SectorResponse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

// --- Request → Command ---

fun SectorCreateRequest.toCommand(tenantId: TenantId) = CreateSectorCommand(
    tenantId = tenantId,
    greenhouseId = GreenhouseId(greenhouseId),
    name = name
)

fun SectorUpdateRequest.toCommand(id: SectorId, tenantId: TenantId) = UpdateSectorCommand(
    id = id,
    tenantId = tenantId,
    greenhouseId = greenhouseId?.let { GreenhouseId(it) },
    name = name
)

// --- Domain → Response ---

fun Sector.toResponse() = SectorResponse(
    id = id?.value ?: throw IllegalStateException("Sector ID cannot be null"),
    code = code,
    tenantId = tenantId.value,
    greenhouseId = greenhouseId.value,
    greenhouseCode = greenhouseCode,
    name = name
)

// --- Entity ↔ Domain ---

fun SectorEntity.toDomain() = Sector(
    id = id?.let { SectorId(it) },
    code = code,
    tenantId = TenantId(tenantId),
    greenhouseId = GreenhouseId(greenhouseId),
    greenhouseCode = greenhouse?.code,
    name = name
)

fun Sector.toEntity() = SectorEntity(
    id = id?.value,
    code = code,
    tenantId = tenantId.value,
    greenhouseId = greenhouseId.value,
    name = name
)

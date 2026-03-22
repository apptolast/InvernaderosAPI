package com.apptolast.invernaderos.features.tenant.dto.mapper

import com.apptolast.invernaderos.features.tenant.Tenant as TenantEntity
import com.apptolast.invernaderos.features.shared.domain.model.Location
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.LocationDto
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus
import com.apptolast.invernaderos.features.tenant.domain.port.input.CreateTenantCommand
import com.apptolast.invernaderos.features.tenant.domain.port.input.UpdateTenantCommand
import com.apptolast.invernaderos.features.tenant.dto.request.TenantCreateRequest
import com.apptolast.invernaderos.features.tenant.dto.request.TenantUpdateRequest
import com.apptolast.invernaderos.features.tenant.dto.response.TenantResponse

// --- Status mapping ---

fun String?.toTenantStatus(): TenantStatus = when (this) {
    "Activo" -> TenantStatus.ACTIVE
    "Inactivo" -> TenantStatus.INACTIVE
    else -> TenantStatus.PENDING
}

fun TenantStatus.toIsActive(): Boolean? = when (this) {
    TenantStatus.ACTIVE -> true
    TenantStatus.INACTIVE -> false
    TenantStatus.PENDING -> null
}

fun TenantStatus.toStatusString(): String = when (this) {
    TenantStatus.ACTIVE -> "Activo"
    TenantStatus.INACTIVE -> "Inactivo"
    TenantStatus.PENDING -> "Pendiente"
}

fun Boolean?.toTenantStatus(): TenantStatus = when (this) {
    true -> TenantStatus.ACTIVE
    false -> TenantStatus.INACTIVE
    null -> TenantStatus.PENDING
}

// --- Request → Command ---

fun TenantCreateRequest.toCommand() = CreateTenantCommand(
    name = name,
    email = email,
    phone = phone,
    province = province,
    country = country,
    location = location?.toDomain(),
    status = status.toTenantStatus()
)

fun TenantUpdateRequest.toCommand(id: TenantId) = UpdateTenantCommand(
    id = id,
    name = name,
    email = email,
    phone = phone,
    province = province,
    country = country,
    location = location?.toDomain(),
    status = status?.toTenantStatus()
)

// --- Domain → Response ---

fun Tenant.toResponse() = TenantResponse(
    id = id?.value ?: throw IllegalStateException("Tenant ID cannot be null"),
    code = code,
    name = name,
    email = email,
    phone = phone,
    province = province,
    country = country,
    location = location?.toLocationDto(),
    isActive = status.toIsActive(),
    status = status.toStatusString()
)

// --- Entity ↔ Domain ---

fun TenantEntity.toDomain() = Tenant(
    id = id?.let { TenantId(it) },
    code = code,
    name = name,
    email = email,
    phone = phone,
    province = province,
    country = country,
    location = location?.toDomain(),
    status = isActive.toTenantStatus(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Tenant.toEntity() = TenantEntity(
    id = id?.value,
    code = code,
    name = name,
    email = email,
    phone = phone,
    province = province,
    country = country,
    location = location?.toLocationDto(),
    isActive = status.toIsActive(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- LocationDto ↔ Location ---

fun LocationDto.toDomain() = Location(lat = lat, lon = lon)

fun Location.toLocationDto() = LocationDto(lat = lat, lon = lon)

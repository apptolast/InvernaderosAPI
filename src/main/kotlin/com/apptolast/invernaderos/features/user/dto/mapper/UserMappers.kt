package com.apptolast.invernaderos.features.user.dto.mapper

import com.apptolast.invernaderos.features.user.User as UserEntity
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.model.UserRole
import com.apptolast.invernaderos.features.user.domain.port.input.CreateUserCommand
import com.apptolast.invernaderos.features.user.domain.port.input.UpdateUserCommand
import com.apptolast.invernaderos.features.user.dto.request.UserCreateRequest
import com.apptolast.invernaderos.features.user.dto.request.UserUpdateRequest
import com.apptolast.invernaderos.features.user.dto.response.UserResponse

// --- Request → Command ---

fun UserCreateRequest.toCommand(tenantId: TenantId) = CreateUserCommand(
    tenantId = tenantId,
    username = username,
    email = email,
    passwordRaw = passwordRaw,
    role = role,
    isActive = isActive
)

fun UserUpdateRequest.toCommand(id: Long, tenantId: TenantId) = UpdateUserCommand(
    id = id,
    tenantId = tenantId,
    username = username,
    email = email,
    passwordRaw = passwordRaw,
    role = role,
    isActive = isActive
)

// --- Domain → Response ---

fun User.toResponse() = UserResponse(
    id = id ?: throw IllegalStateException("User ID cannot be null when mapping to response"),
    code = code,
    tenantId = tenantId.value,
    username = username,
    email = email,
    role = role.name,
    isActive = isActive,
    lastLogin = lastLogin,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Entity ↔ Domain ---

fun UserEntity.toDomain() = User(
    id = id,
    code = code,
    tenantId = TenantId(tenantId),
    username = username,
    email = email,
    role = parseUserRole(role),
    isActive = isActive,
    lastLogin = lastLogin,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun User.toEntity(passwordHash: String) = UserEntity(
    id = id,
    code = code,
    tenantId = tenantId.value,
    username = username,
    email = email,
    passwordHash = passwordHash,
    role = role.name,
    isActive = isActive,
    lastLogin = lastLogin,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun parseUserRole(role: String): UserRole =
    runCatching { UserRole.valueOf(role.uppercase()) }.getOrDefault(UserRole.VIEWER)

// --- JPA Entity → Response (for backward-compat with UserService) ---

fun UserEntity.toResponse() = UserResponse(
    id = id ?: throw IllegalStateException("User ID cannot be null when mapping to response"),
    code = code,
    tenantId = tenantId,
    username = username,
    email = email,
    role = role,
    isActive = isActive,
    lastLogin = lastLogin,
    createdAt = createdAt,
    updatedAt = updatedAt
)

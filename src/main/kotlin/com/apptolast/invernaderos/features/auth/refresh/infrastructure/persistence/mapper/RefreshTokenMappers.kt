package com.apptolast.invernaderos.features.auth.refresh.infrastructure.persistence.mapper

import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshToken
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshTokenFamilyId
import com.apptolast.invernaderos.features.auth.refresh.infrastructure.persistence.entity.RefreshTokenEntity

fun RefreshTokenEntity.toDomain() = RefreshToken(
    id = id,
    userId = userId,
    tokenHash = tokenHash,
    familyId = RefreshTokenFamilyId(familyId),
    rotatedFromId = rotatedFromId,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    createdAt = createdAt
)

fun RefreshToken.toEntity() = RefreshTokenEntity(
    id = id,
    userId = userId,
    tokenHash = tokenHash,
    familyId = familyId.value,
    rotatedFromId = rotatedFromId,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    createdAt = createdAt
)

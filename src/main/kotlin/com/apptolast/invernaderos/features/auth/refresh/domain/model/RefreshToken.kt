package com.apptolast.invernaderos.features.auth.refresh.domain.model

import java.time.Instant

data class RefreshToken(
    val id: Long?,
    val userId: Long,
    val tokenHash: String,
    val familyId: RefreshTokenFamilyId,
    val rotatedFromId: Long?,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant
) {
    fun isExpired(now: Instant): Boolean = !now.isBefore(expiresAt)
    fun isRevoked(): Boolean = revokedAt != null
    fun isUsable(now: Instant): Boolean = !isRevoked() && !isExpired(now)
}

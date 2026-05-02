package com.apptolast.invernaderos.features.auth.refresh.domain.port.output

import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshToken
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshTokenFamilyId
import java.time.Instant

interface RefreshTokenRepositoryPort {
    fun save(token: RefreshToken): RefreshToken

    /**
     * MUST acquire a row-level write lock (SELECT ... FOR UPDATE) so concurrent
     * /refresh calls with the same token serialize and reuse-detection works
     * deterministically.
     */
    fun findByTokenHashLocking(tokenHash: String): RefreshToken?

    fun revoke(id: Long, at: Instant)
    fun revokeFamily(familyId: RefreshTokenFamilyId, at: Instant): Int
    fun revokeAllActiveByUser(userId: Long, at: Instant): Int
}

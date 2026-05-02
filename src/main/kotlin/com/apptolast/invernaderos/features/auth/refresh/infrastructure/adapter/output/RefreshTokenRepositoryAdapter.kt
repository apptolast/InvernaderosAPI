package com.apptolast.invernaderos.features.auth.refresh.infrastructure.adapter.output

import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshToken
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshTokenFamilyId
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.RefreshTokenRepositoryPort
import com.apptolast.invernaderos.features.auth.refresh.infrastructure.persistence.mapper.toDomain
import com.apptolast.invernaderos.features.auth.refresh.infrastructure.persistence.mapper.toEntity
import com.apptolast.invernaderos.features.auth.refresh.infrastructure.persistence.repository.RefreshTokenJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@Transactional("metadataTransactionManager")
class RefreshTokenRepositoryAdapter(
    private val jpa: RefreshTokenJpaRepository
) : RefreshTokenRepositoryPort {

    override fun save(token: RefreshToken): RefreshToken =
        jpa.save(token.toEntity()).toDomain()

    override fun findByTokenHashLocking(tokenHash: String): RefreshToken? =
        jpa.lockByTokenHash(tokenHash)?.toDomain()

    override fun revoke(id: Long, at: Instant) {
        jpa.revokeById(id, at)
    }

    override fun revokeFamily(familyId: RefreshTokenFamilyId, at: Instant): Int =
        jpa.revokeFamily(familyId.value, at)

    override fun revokeAllActiveByUser(userId: Long, at: Instant): Int =
        jpa.revokeAllActiveByUser(userId, at)
}

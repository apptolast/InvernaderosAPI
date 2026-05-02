package com.apptolast.invernaderos.features.auth.refresh.application.usecase

import com.apptolast.invernaderos.features.auth.refresh.domain.error.AuthError
import com.apptolast.invernaderos.features.auth.refresh.domain.model.AuthTokensResult
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenCommand
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.RotateRefreshTokenUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.OpaqueTokenGenerator
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.RefreshTokenRepositoryPort
import org.slf4j.LoggerFactory
import java.time.Clock

class RotateRefreshTokenUseCaseImpl(
    private val repo: RefreshTokenRepositoryPort,
    private val gen: OpaqueTokenGenerator,
    private val issueUseCase: IssueRefreshTokenUseCase,
    private val clock: Clock
) : RotateRefreshTokenUseCase {

    private val log = LoggerFactory.getLogger(RotateRefreshTokenUseCaseImpl::class.java)

    override fun execute(rawRefreshToken: String): Result<AuthTokensResult> {
        if (rawRefreshToken.isBlank()) {
            return Result.failure(AuthErrorException(AuthError.MalformedToken))
        }

        val now = clock.instant()
        val hash = gen.hash(rawRefreshToken)
        val stored = repo.findByTokenHashLocking(hash)
            ?: return Result.failure(AuthErrorException(AuthError.TokenNotFound))

        if (stored.isRevoked()) {
            val revokedRows = repo.revokeFamily(stored.familyId, now)
            log.warn(
                "SECURITY: refresh-token reuse detected — userId={} familyId={} revokedRows={}",
                stored.userId,
                stored.familyId.value,
                revokedRows
            )
            return Result.failure(
                AuthErrorException(AuthError.TokenReuseDetected(stored.familyId.value, stored.userId))
            )
        }

        if (stored.isExpired(now)) {
            return Result.failure(AuthErrorException(AuthError.TokenExpired))
        }

        val parentId = stored.id
            ?: return Result.failure(
                AuthErrorException(AuthError.TokenNotFound)
            ) // defensive: a stored token without id is malformed persistence state
        repo.revoke(parentId, now)

        val tokens = issueUseCase.execute(
            IssueRefreshTokenCommand(
                userId = stored.userId,
                familyId = stored.familyId,
                rotatedFromId = parentId
            )
        )
        return Result.success(tokens)
    }
}

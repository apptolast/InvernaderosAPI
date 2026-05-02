package com.apptolast.invernaderos.features.auth.refresh.application.usecase

import com.apptolast.invernaderos.features.auth.refresh.domain.model.AuthTokensResult
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshToken
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshTokenFamilyId
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenCommand
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.AccessTokenIssuer
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.OpaqueTokenGenerator
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.RefreshTokenRepositoryPort
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.UserClaimsLookup
import java.time.Clock
import java.time.Duration

class IssueRefreshTokenUseCaseImpl(
    private val repo: RefreshTokenRepositoryPort,
    private val gen: OpaqueTokenGenerator,
    private val accessIssuer: AccessTokenIssuer,
    private val userClaims: UserClaimsLookup,
    private val refreshTtl: Duration,
    private val clock: Clock
) : IssueRefreshTokenUseCase {

    override fun execute(cmd: IssueRefreshTokenCommand): AuthTokensResult {
        val now = clock.instant()
        val plainToken = gen.generate()
        val tokenHash = gen.hash(plainToken)

        val familyId = cmd.familyId ?: RefreshTokenFamilyId.new()

        val snapshot = userClaims.loadForToken(cmd.userId)

        val refreshToken = RefreshToken(
            id = null,
            userId = cmd.userId,
            tokenHash = tokenHash,
            familyId = familyId,
            rotatedFromId = null,
            expiresAt = now.plus(refreshTtl),
            revokedAt = null,
            createdAt = now
        )
        repo.save(refreshToken)

        val accessToken = accessIssuer.issue(snapshot.username, snapshot.claims)

        return AuthTokensResult(
            accessToken = accessToken,
            refreshToken = plainToken,
            accessTtlSeconds = accessIssuer.accessTtlSeconds(),
            refreshTtlSeconds = refreshTtl.seconds,
            username = snapshot.username,
            roles = snapshot.roles
        )
    }
}

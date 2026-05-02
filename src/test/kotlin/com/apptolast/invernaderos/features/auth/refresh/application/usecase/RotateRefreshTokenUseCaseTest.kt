package com.apptolast.invernaderos.features.auth.refresh.application.usecase

import com.apptolast.invernaderos.features.auth.refresh.domain.error.AuthError
import com.apptolast.invernaderos.features.auth.refresh.domain.model.AuthTokensResult
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshToken
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshTokenFamilyId
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenCommand
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.OpaqueTokenGenerator
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.RefreshTokenRepositoryPort
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class RotateRefreshTokenUseCaseTest {

    private val repo: RefreshTokenRepositoryPort = mockk(relaxed = true)
    private val gen: OpaqueTokenGenerator = mockk()
    private val issueUseCase: IssueRefreshTokenUseCase = mockk()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-01T12:00:00Z"), ZoneOffset.UTC)

    private val sut = RotateRefreshTokenUseCaseImpl(repo, gen, issueUseCase, clock)

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private val now: Instant = Instant.parse("2026-05-01T12:00:00Z")
    private val familyId = RefreshTokenFamilyId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    private val userId = 42L
    private val tokenHash = "abc123hash"
    private val rawToken = "plain-raw-token"

    private fun buildStoredToken(
        revokedAt: Instant? = null,
        expiresAt: Instant = now.plusSeconds(3600)
    ) = RefreshToken(
        id = 99L,
        userId = userId,
        tokenHash = tokenHash,
        familyId = familyId,
        rotatedFromId = null,
        expiresAt = expiresAt,
        revokedAt = revokedAt,
        createdAt = now.minusSeconds(60)
    )

    private fun buildTokensResult() = AuthTokensResult(
        accessToken = "new-access-jwt",
        refreshToken = "new-plain-refresh",
        accessTtlSeconds = 900L,
        refreshTtlSeconds = 86400L,
        username = "user@example.com",
        roles = listOf("ROLE_USER")
    )

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `should rotate token and inherit family when token is valid and not expired`() {
        val stored = buildStoredToken()
        val expectedTokens = buildTokensResult()

        every { gen.hash(rawToken) } returns tokenHash
        every { repo.findByTokenHashLocking(tokenHash) } returns stored
        every { issueUseCase.execute(IssueRefreshTokenCommand(userId = userId, familyId = familyId)) } returns expectedTokens

        val result = sut.execute(rawToken)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedTokens)

        // Must revoke the consumed token using its Long id
        verify(exactly = 1) { repo.revoke(99L, now) }
        // Must issue new tokens preserving the same familyId
        verify(exactly = 1) { issueUseCase.execute(IssueRefreshTokenCommand(userId = userId, familyId = familyId)) }
        // Must NOT revoke the whole family on a clean rotation
        verify(exactly = 0) { repo.revokeFamily(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // Expired token
    // -------------------------------------------------------------------------

    @Test
    fun `should return TokenExpired failure when stored token has passed its expiry`() {
        val expired = buildStoredToken(expiresAt = now.minusSeconds(1))

        every { gen.hash(rawToken) } returns tokenHash
        every { repo.findByTokenHashLocking(tokenHash) } returns expired

        val result = sut.execute(rawToken)

        assertThat(result.isFailure).isTrue()
        val ex = result.exceptionOrNull() as? AuthErrorException
        assertThat(ex).isNotNull()
        assertThat(ex!!.error).isEqualTo(AuthError.TokenExpired)

        // Expired path must never revoke or issue
        verify(exactly = 0) { repo.revoke(any(), any()) }
        verify(exactly = 0) { issueUseCase.execute(any()) }
    }

    @Test
    fun `should return TokenExpired when token expires exactly at now (boundary)`() {
        // isExpired uses !now.isBefore(expiresAt), so expiresAt == now is expired
        val expired = buildStoredToken(expiresAt = now)

        every { gen.hash(rawToken) } returns tokenHash
        every { repo.findByTokenHashLocking(tokenHash) } returns expired

        val result = sut.execute(rawToken)

        assertThat(result.isFailure).isTrue()
        val ex = result.exceptionOrNull() as? AuthErrorException
        assertThat(ex!!.error).isEqualTo(AuthError.TokenExpired)
    }

    // -------------------------------------------------------------------------
    // Reuse detection
    // -------------------------------------------------------------------------

    @Test
    fun `should revoke entire family and return TokenReuseDetected when token is already revoked`() {
        val revokedAt = now.minusSeconds(30)
        val revoked = buildStoredToken(revokedAt = revokedAt)

        every { gen.hash(rawToken) } returns tokenHash
        every { repo.findByTokenHashLocking(tokenHash) } returns revoked
        every { repo.revokeFamily(familyId, now) } returns 3

        val result = sut.execute(rawToken)

        assertThat(result.isFailure).isTrue()
        val ex = result.exceptionOrNull() as? AuthErrorException
        assertThat(ex).isNotNull()
        val error = ex!!.error
        assertThat(error).isInstanceOf(AuthError.TokenReuseDetected::class.java)
        val reuseError = error as AuthError.TokenReuseDetected
        assertThat(reuseError.familyId).isEqualTo(familyId.value)
        assertThat(reuseError.userId).isEqualTo(userId)

        // Full family revocation must happen
        verify(exactly = 1) { repo.revokeFamily(familyId, now) }
        // Must NOT revoke a single token nor issue new tokens
        verify(exactly = 0) { repo.revoke(any(), any()) }
        verify(exactly = 0) { issueUseCase.execute(any()) }
    }

    // -------------------------------------------------------------------------
    // Token not found
    // -------------------------------------------------------------------------

    @Test
    fun `should return TokenNotFound failure when hash does not match any stored token`() {
        every { gen.hash(rawToken) } returns tokenHash
        every { repo.findByTokenHashLocking(tokenHash) } returns null

        val result = sut.execute(rawToken)

        assertThat(result.isFailure).isTrue()
        val ex = result.exceptionOrNull() as? AuthErrorException
        assertThat(ex).isNotNull()
        assertThat(ex!!.error).isEqualTo(AuthError.TokenNotFound)

        verify(exactly = 0) { repo.revoke(any(), any()) }
        verify(exactly = 0) { issueUseCase.execute(any()) }
    }

    // -------------------------------------------------------------------------
    // Malformed / blank token
    // -------------------------------------------------------------------------

    @Test
    fun `should return MalformedToken failure for empty string without touching repo or generator`() {
        val result = sut.execute("")

        assertThat(result.isFailure).isTrue()
        val ex = result.exceptionOrNull() as? AuthErrorException
        assertThat(ex).isNotNull()
        assertThat(ex!!.error).isEqualTo(AuthError.MalformedToken)

        verify(exactly = 0) { gen.hash(any()) }
        verify(exactly = 0) { repo.findByTokenHashLocking(any()) }
        confirmVerified(gen, repo, issueUseCase)
    }

    @Test
    fun `should return MalformedToken failure for blank whitespace-only token`() {
        val result = sut.execute("   ")

        assertThat(result.isFailure).isTrue()
        val ex = result.exceptionOrNull() as? AuthErrorException
        assertThat(ex!!.error).isEqualTo(AuthError.MalformedToken)

        verify(exactly = 0) { gen.hash(any()) }
        verify(exactly = 0) { repo.findByTokenHashLocking(any()) }
        confirmVerified(gen, repo, issueUseCase)
    }

    @Test
    fun `should return MalformedToken failure for tab-only token`() {
        val result = sut.execute("\t")

        assertThat(result.isFailure).isTrue()
        val ex = result.exceptionOrNull() as? AuthErrorException
        assertThat(ex!!.error).isEqualTo(AuthError.MalformedToken)

        verify(exactly = 0) { gen.hash(any()) }
        verify(exactly = 0) { repo.findByTokenHashLocking(any()) }
    }
}

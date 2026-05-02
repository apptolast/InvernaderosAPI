package com.apptolast.invernaderos.features.auth.refresh.application.usecase

import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshToken
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshTokenFamilyId
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenCommand
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.AccessTokenIssuer
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.OpaqueTokenGenerator
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.RefreshTokenRepositoryPort
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.UserClaimsLookup
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class IssueRefreshTokenUseCaseTest {

    private val repo: RefreshTokenRepositoryPort = mockk(relaxed = true)
    private val gen: OpaqueTokenGenerator = mockk()
    private val accessIssuer: AccessTokenIssuer = mockk()
    private val userClaims: UserClaimsLookup = mockk()

    private val now: Instant = Instant.parse("2026-05-01T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val refreshTtl: Duration = Duration.ofSeconds(86400)

    private val sut = IssueRefreshTokenUseCaseImpl(repo, gen, accessIssuer, userClaims, refreshTtl, clock)

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private val userId = 7L
    private val plainToken = "plain-opaque-token"
    private val tokenHash = "sha256hexhash0000000000000000000000000000000000000000000000000000"
    private val accessJwt = "header.payload.signature"
    private val accessTtl = 900L
    private val username = "farmer@greenhouse.io"
    private val roles = listOf("ROLE_USER")
    private val claims = mapOf<String, Any>("tenantId" to 3L, "role" to "USER")

    private fun stubGenerator() {
        every { gen.generate() } returns plainToken
        every { gen.hash(plainToken) } returns tokenHash
    }

    private fun stubUserClaims() {
        every { userClaims.loadForToken(userId) } returns
            UserClaimsLookup.UserClaimsSnapshot(username = username, roles = roles, claims = claims)
    }

    private fun stubAccessIssuer() {
        every { accessIssuer.issue(username, claims) } returns accessJwt
        every { accessIssuer.accessTtlSeconds() } returns accessTtl
    }

    // -------------------------------------------------------------------------
    // New family (login / register flow)
    // -------------------------------------------------------------------------

    @Test
    fun `should generate new familyId when command carries no familyId`() {
        stubGenerator()
        stubUserClaims()
        stubAccessIssuer()

        val savedSlot = slot<RefreshToken>()
        every { repo.save(capture(savedSlot)) } answers { firstArg() }

        val cmd = IssueRefreshTokenCommand(userId = userId, familyId = null)
        val result = sut.execute(cmd)

        val saved = savedSlot.captured
        // A new UUID-based familyId must have been generated (not null, not the command's null)
        assertThat(saved.familyId).isNotNull()
        assertThat(saved.familyId.value).isInstanceOf(UUID::class.java)
    }

    @Test
    fun `should persist token with correct fields when issuing for a new family`() {
        stubGenerator()
        stubUserClaims()
        stubAccessIssuer()

        val savedSlot = slot<RefreshToken>()
        every { repo.save(capture(savedSlot)) } answers { firstArg() }

        val cmd = IssueRefreshTokenCommand(userId = userId, familyId = null)
        sut.execute(cmd)

        val saved = savedSlot.captured
        assertThat(saved.userId).isEqualTo(userId)
        assertThat(saved.tokenHash).isEqualTo(tokenHash)
        assertThat(saved.revokedAt).isNull()
        assertThat(saved.expiresAt).isEqualTo(now.plus(refreshTtl))
        assertThat(saved.createdAt).isEqualTo(now)
        assertThat(saved.id).isNull() // id is assigned by DB, must be null on save
    }

    @Test
    fun `should return AuthTokensResult with correct TTLs and credentials when issuing new family`() {
        stubGenerator()
        stubUserClaims()
        stubAccessIssuer()
        every { repo.save(any()) } answers { firstArg() }

        val cmd = IssueRefreshTokenCommand(userId = userId, familyId = null)
        val result = sut.execute(cmd)

        assertThat(result.accessToken).isEqualTo(accessJwt)
        assertThat(result.refreshToken).isEqualTo(plainToken)
        assertThat(result.accessTtlSeconds).isEqualTo(accessTtl)
        assertThat(result.refreshTtlSeconds).isEqualTo(refreshTtl.seconds)
        assertThat(result.username).isEqualTo(username)
        assertThat(result.roles).isEqualTo(roles)
    }

    // -------------------------------------------------------------------------
    // Inherited family (rotation flow)
    // -------------------------------------------------------------------------

    @Test
    fun `should persist token with exact inherited familyId when command carries an existing familyId`() {
        stubGenerator()
        stubUserClaims()
        stubAccessIssuer()

        val inheritedFamilyId = RefreshTokenFamilyId(UUID.fromString("cafebabe-cafe-babe-cafe-babecafebabe"))

        val savedSlot = slot<RefreshToken>()
        every { repo.save(capture(savedSlot)) } answers { firstArg() }

        val cmd = IssueRefreshTokenCommand(userId = userId, familyId = inheritedFamilyId)
        sut.execute(cmd)

        val saved = savedSlot.captured
        assertThat(saved.familyId).isEqualTo(inheritedFamilyId)
    }

    @Test
    fun `should return plaintext token in result so caller can send it to the client`() {
        stubGenerator()
        stubUserClaims()
        stubAccessIssuer()
        every { repo.save(any()) } answers { firstArg() }

        val cmd = IssueRefreshTokenCommand(userId = userId, familyId = null)
        val result = sut.execute(cmd)

        // plainToken must appear in the result (for the client), not the hash
        assertThat(result.refreshToken).isEqualTo(plainToken)
        assertThat(result.refreshToken).isNotEqualTo(tokenHash)
    }

    @Test
    fun `should invoke generate then hash in order and call repo save exactly once`() {
        stubGenerator()
        stubUserClaims()
        stubAccessIssuer()
        every { repo.save(any()) } answers { firstArg() }

        val cmd = IssueRefreshTokenCommand(userId = userId, familyId = null)
        sut.execute(cmd)

        verify(exactly = 1) { gen.generate() }
        verify(exactly = 1) { gen.hash(plainToken) }
        verify(exactly = 1) { repo.save(any()) }
        verify(exactly = 1) { accessIssuer.issue(username, claims) }
    }
}

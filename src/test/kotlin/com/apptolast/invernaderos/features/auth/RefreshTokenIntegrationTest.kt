package com.apptolast.invernaderos.features.auth

import com.apptolast.invernaderos.features.auth.dto.request.RefreshRequest
import com.apptolast.invernaderos.features.auth.dto.response.JwtResponse
import com.apptolast.invernaderos.features.auth.refresh.application.usecase.AuthErrorException
import com.apptolast.invernaderos.features.auth.refresh.domain.error.AuthError
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * HTTP contract tests for POST /api/v1/auth/refresh.
 *
 * Strategy: mock AuthRefreshService at the Spring bean level (@MockBean) so
 * the full Spring Security / MockMvc / Jackson / validation stack is exercised
 * without touching the DEV database or any real use case wiring.
 *
 * All assertions verify the *HTTP contract*: status code + JSON shape.
 * Business-logic correctness is covered by RotateRefreshTokenUseCaseTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    /**
     * Mocking AuthRefreshService instead of the individual use cases because
     * AuthController -> AuthService -> AuthRefreshService.refresh() is the
     * single call site that produces the Result<JwtResponse> the controller
     * unwraps into HTTP status codes.
     */
    @MockitoBean private lateinit var authRefreshService: AuthRefreshService

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private val validJwtResponse = JwtResponse(
        token = "header.payload.sig",
        type = "Bearer",
        username = "farmer@greenhouse.io",
        roles = listOf("ROLE_USER"),
        refreshToken = "new-opaque-refresh-token",
        expiresIn = 900L,
        refreshExpiresIn = 86400L
    )

    private fun refreshBody(token: String) =
        objectMapper.writeValueAsString(mapOf("refreshToken" to token))

    private fun stubRefreshSuccess() {
        `when`(authRefreshService.refresh(RefreshRequest("valid-token")))
            .thenReturn(Result.success(validJwtResponse))
    }

    private fun stubRefreshFailure(token: String, error: AuthError) {
        `when`(authRefreshService.refresh(RefreshRequest(token)))
            .thenReturn(Result.failure(AuthErrorException(error)))
    }

    // -------------------------------------------------------------------------
    // 200 — successful rotation
    // -------------------------------------------------------------------------

    @Test
    fun `should return 200 with full JwtResponse when token rotates successfully`() {
        stubRefreshSuccess()

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("valid-token"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("header.payload.sig"))
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.username").value("farmer@greenhouse.io"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
            .andExpect(jsonPath("$.refreshToken").value("new-opaque-refresh-token"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.refreshExpiresIn").value(86400))
    }

    // -------------------------------------------------------------------------
    // 401 — invalid / expired / revoked / reused
    // -------------------------------------------------------------------------

    @Test
    fun `should return 401 when token is not found in the store`() {
        stubRefreshFailure("unknown-token", AuthError.TokenNotFound)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("unknown-token"))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 when token is expired`() {
        stubRefreshFailure("expired-token", AuthError.TokenExpired)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("expired-token"))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 when token was already explicitly revoked`() {
        stubRefreshFailure("revoked-token", AuthError.TokenRevoked)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("revoked-token"))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 when reuse of a revoked token is detected and whole family is revoked`() {
        val familyId = UUID.fromString("cafebabe-cafe-babe-cafe-babecafebabe")
        stubRefreshFailure("reused-token", AuthError.TokenReuseDetected(familyId = familyId, userId = 42L))

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("reused-token"))
        )
            .andExpect(status().isUnauthorized)
    }

    // -------------------------------------------------------------------------
    // 400 — malformed token value (business-level, not Jakarta validation)
    // -------------------------------------------------------------------------

    @Test
    fun `should return 400 when the refresh token string is malformed according to business rules`() {
        stubRefreshFailure("bad-token", AuthError.MalformedToken)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("bad-token"))
        )
            .andExpect(status().isBadRequest)
    }

    // -------------------------------------------------------------------------
    // 503 — feature disabled
    // -------------------------------------------------------------------------

    @Test
    fun `should return 503 when the refresh token feature is administratively disabled`() {
        stubRefreshFailure("any-token", AuthError.FeatureDisabled)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("any-token"))
        )
            .andExpect(status().isServiceUnavailable)
    }

    // -------------------------------------------------------------------------
    // 400 — Jakarta @NotBlank validation (before any use case is invoked)
    // -------------------------------------------------------------------------

    @Test
    fun `should return 400 when refreshToken field is present but blank`() {
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":""}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 when refreshToken field is missing from request body`() {
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 when request body is entirely absent`() {
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }
}

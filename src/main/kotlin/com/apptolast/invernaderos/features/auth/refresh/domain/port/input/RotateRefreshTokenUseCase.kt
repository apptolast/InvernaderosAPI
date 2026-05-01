package com.apptolast.invernaderos.features.auth.refresh.domain.port.input

import com.apptolast.invernaderos.features.auth.refresh.domain.model.AuthTokensResult

interface RotateRefreshTokenUseCase {
    /**
     * Validates and rotates an opaque refresh token.
     * Returns Result.success with the new token pair, or Result.failure(AuthErrorException(error))
     * containing the AuthError as cause.
     * Reuse of a revoked token triggers full-family revocation (the failure carries TokenReuseDetected).
     */
    fun execute(rawRefreshToken: String): Result<AuthTokensResult>
}

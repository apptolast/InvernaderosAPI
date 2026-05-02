package com.apptolast.invernaderos.features.auth.refresh.domain.port.input

interface RevokeUserRefreshTokensUseCase {
    /** Revokes all active refresh tokens of a user. Returns the number of rows revoked. */
    fun execute(userId: Long): Int
}

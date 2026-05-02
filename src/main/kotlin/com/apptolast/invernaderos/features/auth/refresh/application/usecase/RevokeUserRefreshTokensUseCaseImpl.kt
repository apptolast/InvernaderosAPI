package com.apptolast.invernaderos.features.auth.refresh.application.usecase

import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.RevokeUserRefreshTokensUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.RefreshTokenRepositoryPort
import java.time.Clock

class RevokeUserRefreshTokensUseCaseImpl(
    private val repo: RefreshTokenRepositoryPort,
    private val clock: Clock
) : RevokeUserRefreshTokensUseCase {

    override fun execute(userId: Long): Int {
        return repo.revokeAllActiveByUser(userId, clock.instant())
    }
}

package com.apptolast.invernaderos.features.auth.refresh.infrastructure.config

import com.apptolast.invernaderos.features.auth.refresh.application.usecase.IssueRefreshTokenUseCaseImpl
import com.apptolast.invernaderos.features.auth.refresh.application.usecase.RevokeUserRefreshTokensUseCaseImpl
import com.apptolast.invernaderos.features.auth.refresh.application.usecase.RotateRefreshTokenUseCaseImpl
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.RevokeUserRefreshTokensUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.RotateRefreshTokenUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.AccessTokenIssuer
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.OpaqueTokenGenerator
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.RefreshTokenRepositoryPort
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.UserClaimsLookup
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

@Configuration
class RefreshTokenModuleConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun issueRefreshTokenUseCase(
        repo: RefreshTokenRepositoryPort,
        gen: OpaqueTokenGenerator,
        accessIssuer: AccessTokenIssuer,
        userClaims: UserClaimsLookup,
        @Value("\${spring.security.jwt.refresh-expiration}") refreshMs: Long,
        clock: Clock
    ): IssueRefreshTokenUseCase = IssueRefreshTokenUseCaseImpl(
        repo, gen, accessIssuer, userClaims, Duration.ofMillis(refreshMs), clock
    )

    @Bean
    fun rotateRefreshTokenUseCase(
        repo: RefreshTokenRepositoryPort,
        gen: OpaqueTokenGenerator,
        issue: IssueRefreshTokenUseCase,
        clock: Clock
    ): RotateRefreshTokenUseCase = RotateRefreshTokenUseCaseImpl(repo, gen, issue, clock)

    @Bean
    fun revokeUserRefreshTokensUseCase(
        repo: RefreshTokenRepositoryPort,
        clock: Clock
    ): RevokeUserRefreshTokensUseCase = RevokeUserRefreshTokensUseCaseImpl(repo, clock)
}

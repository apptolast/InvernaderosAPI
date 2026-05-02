package com.apptolast.invernaderos.core.security

import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.RevokeUserRefreshTokensUseCase
import com.apptolast.invernaderos.features.user.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Component

@Component
class JwtLogoutHandler(
    private val revokeUserRefreshTokensUseCase: RevokeUserRefreshTokensUseCase,
    private val userService: UserService,
    @Value("\${auth.refresh-token.enabled:true}") private val refreshEnabled: Boolean
) : LogoutHandler {

    private val log = LoggerFactory.getLogger(JwtLogoutHandler::class.java)

    override fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
        auth: Authentication?
    ) {
        if (!refreshEnabled || auth == null) return
        val username = auth.name ?: return
        val user = userService.findByEmail(username) ?: return
        val n = revokeUserRefreshTokensUseCase.execute(user.id!!)
        log.info("Logout: revoked {} active refresh tokens for userId={}", n, user.id)
    }
}

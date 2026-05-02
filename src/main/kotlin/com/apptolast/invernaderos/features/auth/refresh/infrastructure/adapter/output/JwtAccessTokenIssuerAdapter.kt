package com.apptolast.invernaderos.features.auth.refresh.infrastructure.adapter.output

import com.apptolast.invernaderos.core.security.JwtService
import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.AccessTokenIssuer
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component

@Component
class JwtAccessTokenIssuerAdapter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : AccessTokenIssuer {

    override fun issue(username: String, extraClaims: Map<String, Any>): String {
        val ud = userDetailsService.loadUserByUsername(username)
        return jwtService.generateToken(extraClaims, ud)
    }

    override fun accessTtlSeconds(): Long = jwtService.getAccessTtlSeconds()
}

package com.apptolast.invernaderos.features.auth.refresh.infrastructure.adapter.output

import com.apptolast.invernaderos.features.auth.refresh.domain.port.output.UserClaimsLookup
import com.apptolast.invernaderos.features.user.UserService
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component

@Component
class UserClaimsLookupAdapter(
    private val userService: UserService,
    private val userDetailsService: UserDetailsService
) : UserClaimsLookup {

    override fun loadForToken(userId: Long): UserClaimsLookup.UserClaimsSnapshot {
        val user = userService.findById(userId)
            ?: throw IllegalStateException("User $userId not found while issuing refresh token")
        val ud = userDetailsService.loadUserByUsername(user.email)
        return UserClaimsLookup.UserClaimsSnapshot(
            username = user.email,
            roles = ud.authorities.map { it.authority },
            claims = mapOf("tenantId" to user.tenantId, "role" to user.role)
        )
    }
}

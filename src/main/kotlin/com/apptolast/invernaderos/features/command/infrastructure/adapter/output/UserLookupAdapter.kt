package com.apptolast.invernaderos.features.command.infrastructure.adapter.output

import com.apptolast.invernaderos.features.command.domain.port.output.UserInfo
import com.apptolast.invernaderos.features.command.domain.port.output.UserLookupPort
import com.apptolast.invernaderos.features.user.UserRepository
import org.springframework.stereotype.Component

@Component
class UserLookupAdapter(
    private val userRepository: UserRepository
) : UserLookupPort {
    override fun findUserByEmail(email: String): UserInfo? {
        val user = userRepository.findByEmail(email) ?: return null
        return UserInfo(userId = user.id!!, tenantId = user.tenantId)
    }
}

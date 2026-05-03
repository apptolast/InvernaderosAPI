package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationUserSnapshot
import com.apptolast.invernaderos.features.notification.domain.port.output.UserLookupPort
import com.apptolast.invernaderos.features.user.UserRepository
import org.springframework.stereotype.Component

@Component
class UserLookupAdapter(
    private val userRepository: UserRepository
) : UserLookupPort {

    override fun findById(userId: Long): NotificationUserSnapshot? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        return NotificationUserSnapshot(
            id = user.id!!,
            username = user.username,
            displayName = user.username,
            locale = user.locale,
            tenantId = user.tenantId
        )
    }
}

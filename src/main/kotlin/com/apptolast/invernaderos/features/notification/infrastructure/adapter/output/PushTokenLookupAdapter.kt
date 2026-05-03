package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationTokenSnapshot
import com.apptolast.invernaderos.features.notification.domain.port.output.PushTokenLookupPort
import com.apptolast.invernaderos.features.push.PushTokenRepository
import org.springframework.stereotype.Component

@Component
class PushTokenLookupAdapter(
    private val pushTokenRepository: PushTokenRepository
) : PushTokenLookupPort {

    override fun findActiveTokensForTenant(tenantId: Long): List<NotificationTokenSnapshot> =
        pushTokenRepository.findAllByTenantId(tenantId).map { token ->
            NotificationTokenSnapshot(
                id = token.id!!,
                userId = token.userId,
                token = token.token,
                platform = token.platform.name
            )
        }
}

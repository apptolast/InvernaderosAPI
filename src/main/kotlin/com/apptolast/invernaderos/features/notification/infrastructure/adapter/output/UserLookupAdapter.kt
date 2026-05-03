package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationUserSnapshot
import com.apptolast.invernaderos.features.notification.domain.port.output.UserLookupPort
import com.apptolast.invernaderos.features.user.User
import com.apptolast.invernaderos.features.user.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component("notificationUserLookupAdapter")
class UserLookupAdapter(
    private val userRepository: UserRepository
) : UserLookupPort {

    override fun findById(userId: Long): NotificationUserSnapshot? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        return user.toSnapshot()
    }

    override fun findByPrincipalName(principalName: String): NotificationUserSnapshot? {
        val user = userRepository.findByEmail(principalName)
            ?: userRepository.findByUsername(principalName)
            ?: return null
        return user.toSnapshot()
    }

    @Transactional("metadataTransactionManager")
    override fun updateLocale(userId: Long, locale: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        if (user.locale == locale) return true
        user.locale = locale
        userRepository.save(user)
        return true
    }

    private fun User.toSnapshot(): NotificationUserSnapshot =
        NotificationUserSnapshot(
            id = id!!,
            username = username,
            displayName = username,
            locale = locale,
            tenantId = tenantId
        )
}

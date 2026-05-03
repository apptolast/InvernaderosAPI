package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.PreferredChannelJpa
import com.apptolast.invernaderos.features.notification.UserNotificationPreferencesEntity
import com.apptolast.invernaderos.features.notification.UserNotificationPreferencesJpaRepository
import com.apptolast.invernaderos.features.notification.domain.model.PreferredChannel
import com.apptolast.invernaderos.features.notification.domain.model.QuietHours
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId

@Component
class UserPreferencesPersistenceAdapter(
    private val jpaRepository: UserNotificationPreferencesJpaRepository
) : UserPreferencesRepositoryPort {

    override fun findByUserId(userId: Long): UserNotificationPreferences? =
        jpaRepository.findById(userId).orElse(null)?.toDomain()

    override fun save(preferences: UserNotificationPreferences): UserNotificationPreferences {
        val existing = jpaRepository.findById(preferences.userId).orElse(null)
        val entity = preferences.toEntity(updatedAt = Instant.now(), createdAt = existing?.createdAt ?: Instant.now())
        return jpaRepository.save(entity).toDomain()
    }
}

private fun UserNotificationPreferencesEntity.toDomain(): UserNotificationPreferences =
    UserNotificationPreferences(
        userId = userId,
        categoryAlerts = categoryAlerts,
        categoryDevices = categoryDevices,
        categorySubscription = categorySubscription,
        minAlertSeverity = minAlertSeverity.toInt(),
        quietHours = QuietHours(
            start = quietHoursStart,
            end = quietHoursEnd,
            timezone = ZoneId.of(quietHoursTimezone)
        ),
        preferredChannel = PreferredChannel.valueOf(preferredChannel.name)
    )

private fun UserNotificationPreferences.toEntity(
    updatedAt: Instant,
    createdAt: Instant
): UserNotificationPreferencesEntity =
    UserNotificationPreferencesEntity(
        userId = userId,
        categoryAlerts = categoryAlerts,
        categoryDevices = categoryDevices,
        categorySubscription = categorySubscription,
        minAlertSeverity = minAlertSeverity.toShort(),
        quietHoursStart = quietHours.start,
        quietHoursEnd = quietHours.end,
        quietHoursTimezone = quietHours.timezone.id,
        preferredChannel = PreferredChannelJpa.valueOf(preferredChannel.name),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

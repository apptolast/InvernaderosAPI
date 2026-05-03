package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.NotificationLogEntity
import com.apptolast.invernaderos.features.notification.NotificationLogJpaRepository
import com.apptolast.invernaderos.features.notification.domain.model.NotificationLogEntry
import com.apptolast.invernaderos.features.notification.domain.model.NotificationStatus
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationLogPage
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationLogRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class NotificationLogPersistenceAdapter(
    private val jpaRepository: NotificationLogJpaRepository
) : NotificationLogRepositoryPort {

    override fun save(entry: NotificationLogEntry): NotificationLogEntry {
        val entity = entry.toEntity()
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun listForUser(userId: Long, cursor: Long?, limit: Int): UserNotificationLogPage {
        val rows = jpaRepository.listForUser(userId, cursor, PageRequest.of(0, limit))
        val entries = rows.map { it.toDomain() }
        val nextCursor = if (entries.size == limit) entries.last().id else null
        return UserNotificationLogPage(entries = entries, nextCursor = nextCursor)
    }

    override fun hasRecentSent(notificationType: NotificationType, alertId: Long, sinceInstant: Instant): Boolean =
        jpaRepository.hasRecentSentForAlert(notificationType.name, alertId, sinceInstant)
}

private fun NotificationLogEntry.toEntity(): NotificationLogEntity =
    NotificationLogEntity(
        id = id,
        tenantId = tenantId,
        userId = userId,
        deviceTokenId = deviceTokenId,
        notificationType = notificationType.name,
        payloadJson = payloadJson,
        status = status.name,
        fcmMessageId = fcmMessageId,
        error = error,
        sentAt = sentAt
    )

private fun NotificationLogEntity.toDomain(): NotificationLogEntry =
    NotificationLogEntry(
        id = id,
        tenantId = tenantId,
        userId = userId,
        deviceTokenId = deviceTokenId,
        notificationType = NotificationType.valueOf(notificationType),
        payloadJson = payloadJson,
        status = NotificationStatus.valueOf(status),
        fcmMessageId = fcmMessageId,
        error = error,
        sentAt = sentAt
    )

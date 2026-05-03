package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationLogPage
import com.apptolast.invernaderos.features.notification.domain.port.input.ListUserNotificationsUseCase
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationLogRepositoryPort

class ListUserNotificationsUseCaseImpl(
    private val notificationLogRepository: NotificationLogRepositoryPort
) : ListUserNotificationsUseCase {

    override fun list(userId: Long, cursor: Long?, limit: Int): UserNotificationLogPage =
        notificationLogRepository.listForUser(userId, cursor, limit)
}

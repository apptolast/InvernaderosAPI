package com.apptolast.invernaderos.features.notification.infrastructure.config

import com.apptolast.invernaderos.features.notification.application.usecase.DispatchNotificationUseCaseImpl
import com.apptolast.invernaderos.features.notification.application.usecase.GetUserPreferencesUseCaseImpl
import com.apptolast.invernaderos.features.notification.application.usecase.ListUserNotificationsUseCaseImpl
import com.apptolast.invernaderos.features.notification.application.usecase.UpdateUserPreferencesUseCaseImpl
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.port.input.DispatchNotificationUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.GetUserPreferencesUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.ListUserNotificationsUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.UpdateUserPreferencesUseCase
import com.apptolast.invernaderos.features.notification.domain.port.output.AlertSeverityLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.FcmSenderPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationContentRendererPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationDedupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationLogRepositoryPort
import com.apptolast.invernaderos.features.notification.domain.port.output.PushTokenLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.UserLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(NotificationProperties::class)
class NotificationModuleConfig {

    @Bean
    fun dispatchNotificationUseCase(
        alertSeverityLookup: AlertSeverityLookupPort,
        pushTokenLookup: PushTokenLookupPort,
        userLookup: UserLookupPort,
        preferencesRepository: UserPreferencesRepositoryPort,
        notificationDedupPort: NotificationDedupPort,
        contentRenderer: NotificationContentRendererPort,
        fcmSender: FcmSenderPort,
        notificationLogRepository: NotificationLogRepositoryPort,
        props: NotificationProperties
    ): DispatchNotificationUseCase = DispatchNotificationUseCaseImpl(
        alertSeverityLookup = alertSeverityLookup,
        pushTokenLookup = pushTokenLookup,
        userLookup = userLookup,
        preferencesRepository = preferencesRepository,
        notificationDedupPort = notificationDedupPort,
        contentRenderer = contentRenderer,
        fcmSender = fcmSender,
        notificationLogRepository = notificationLogRepository,
        dedupWindowsByType = mapOf(
            NotificationType.ALERT_ACTIVATED to props.dedup.window.alertActivated,
            NotificationType.ALERT_RESOLVED to props.dedup.window.alertResolved
        )
    )

    @Bean
    fun getUserPreferencesUseCase(
        preferencesRepository: UserPreferencesRepositoryPort
    ): GetUserPreferencesUseCase = GetUserPreferencesUseCaseImpl(preferencesRepository)

    @Bean
    fun updateUserPreferencesUseCase(
        preferencesRepository: UserPreferencesRepositoryPort
    ): UpdateUserPreferencesUseCase = UpdateUserPreferencesUseCaseImpl(preferencesRepository)

    @Bean
    fun listUserNotificationsUseCase(
        notificationLogRepository: NotificationLogRepositoryPort
    ): ListUserNotificationsUseCase = ListUserNotificationsUseCaseImpl(notificationLogRepository)
}

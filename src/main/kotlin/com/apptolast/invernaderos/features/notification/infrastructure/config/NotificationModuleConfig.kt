package com.apptolast.invernaderos.features.notification.infrastructure.config

import com.apptolast.invernaderos.features.notification.application.usecase.DetectAgingAlertsUseCaseImpl
import com.apptolast.invernaderos.features.notification.application.usecase.DispatchNotificationUseCaseImpl
import com.apptolast.invernaderos.features.notification.application.usecase.GetUserPreferencesUseCaseImpl
import com.apptolast.invernaderos.features.notification.application.usecase.ListUserNotificationsUseCaseImpl
import com.apptolast.invernaderos.features.notification.application.usecase.UpdateUserPreferencesUseCaseImpl
import com.apptolast.invernaderos.features.notification.domain.model.AgingThresholdsConfig
import com.apptolast.invernaderos.features.notification.domain.port.input.DetectAgingAlertsUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.DispatchNotificationUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.GetUserPreferencesUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.ListUserNotificationsUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.UpdateUserPreferencesUseCase
import com.apptolast.invernaderos.features.notification.domain.port.output.AgingAlertScannerPort
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
import java.time.Duration

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
        dedupWindowSeconds = props.dedup.window.alertActivated.seconds
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

    @Bean
    fun detectAgingAlertsUseCase(
        agingAlertScanner: AgingAlertScannerPort,
        notificationLogRepository: NotificationLogRepositoryPort,
        agingThresholdsConfig: AgingThresholdsConfig
    ): DetectAgingAlertsUseCase = DetectAgingAlertsUseCaseImpl(
        agingAlertScanner = agingAlertScanner,
        notificationLogRepository = notificationLogRepository,
        thresholdsConfig = agingThresholdsConfig
    )

    /**
     * Maps the severity names from [NotificationProperties.AgingProps.thresholds]
     * to the numeric severity levels used by [AgingThresholdsConfig].
     *
     * Level mapping follows the AlertSeverity catalog:
     *   INFO=1, WARNING=2, ERROR=3, CRITICAL=4
     */
    @Bean
    fun agingThresholdsConfig(props: NotificationProperties): AgingThresholdsConfig {
        val levelByName: Map<String, Short> = mapOf(
            "INFO" to 1,
            "WARNING" to 2,
            "ERROR" to 3,
            "CRITICAL" to 4
        )
        val thresholds: Map<Short, Duration> = props.aging.thresholds
            .mapNotNull { (name, duration) ->
                val level = levelByName[name.uppercase()]
                if (level != null) level to duration else null
            }
            .toMap()
        return AgingThresholdsConfig(thresholds)
    }
}

package com.apptolast.invernaderos.features.notification.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "notification")
data class NotificationProperties(
    val aging: AgingProps = AgingProps(),
    val dedup: DedupProps = DedupProps(),
    val log: LogProps = LogProps(),
    val fcm: FcmProps = FcmProps(),
    val i18n: I18nProps = I18nProps()
) {
    data class AgingProps(
        val scanInterval: Duration = Duration.ofMinutes(5),
        val thresholds: Map<String, Duration> = mapOf(
            "CRITICAL" to Duration.ofMinutes(30),
            "ERROR" to Duration.ofHours(2),
            "WARNING" to Duration.ofHours(8)
        )
    )

    data class DedupProps(
        val enabled: Boolean = true,
        val window: WindowProps = WindowProps()
    )

    data class WindowProps(
        val alertActivated: Duration = Duration.ofSeconds(60),
        val alertResolved: Duration = Duration.ofSeconds(60),
        val alertAging: Duration = Duration.ofMinutes(30)
    )

    data class LogProps(val retentionDays: Int = 90)

    data class FcmProps(val retry: RetryProps = RetryProps())

    data class RetryProps(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 500,
        val multiplier: Double = 2.0,
        val maxDelayMs: Long = 5000
    )

    data class I18nProps(
        val defaultLocale: String = "es-ES",
        val supportedLocales: List<String> = listOf("es-ES", "en-US")
    )
}

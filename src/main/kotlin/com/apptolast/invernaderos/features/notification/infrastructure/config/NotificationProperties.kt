package com.apptolast.invernaderos.features.notification.infrastructure.config

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@ConfigurationProperties(prefix = "notification")
@Validated
data class NotificationProperties(
    val dedup: DedupProps = DedupProps(),
    @field:Valid val log: LogProps = LogProps(),
    val fcm: FcmProps = FcmProps(),
    val i18n: I18nProps = I18nProps()
) {
    data class DedupProps(
        val enabled: Boolean = true,
        val window: WindowProps = WindowProps()
    )

    data class WindowProps(
        val alertActivated: Duration = Duration.ofSeconds(60),
        val alertResolved: Duration = Duration.ofSeconds(60)
    )

    data class LogProps(@field:Min(1) val retentionDays: Int = 90)

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

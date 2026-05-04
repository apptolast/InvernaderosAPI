package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.notification.domain.model.NotificationRecipient
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationSeveritySnapshot
import com.apptolast.invernaderos.features.notification.infrastructure.config.NotificationProperties
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticMessageSource
import java.time.Instant
import java.util.Locale

class MessageSourceContentRendererAdapterTest {

    private val defaultLocale = Locale.forLanguageTag("en-US")
    private val messageSource = StaticMessageSource().apply {
        addMessage("notification.alert.resolved.title", defaultLocale, "Alert resolved: {0}")
        addMessage("notification.alert.resolved.body", defaultLocale, "{0} resolved by {1}")
        addMessage("notification.actor.system", defaultLocale, "system")
    }

    private val adapter = MessageSourceContentRendererAdapter(
        messageSource = messageSource,
        props = NotificationProperties(
            i18n = NotificationProperties.I18nProps(
                defaultLocale = "en-US",
                supportedLocales = listOf("en-US", "es-ES")
            )
        )
    )

    @Test
    fun `should render unsupported recipient locale with configured default locale`() {
        val content = adapter.render(
            type = NotificationType.ALERT_RESOLVED,
            alert = alert(),
            change = change(),
            recipient = NotificationRecipient(
                userId = 50L,
                tokenId = 100L,
                tokenValue = "token",
                locale = Locale.FRANCE
            ),
            severity = severity()
        )

        assertThat(content.title).isEqualTo("Alert resolved: ALT-00001")
        assertThat(content.body).isEqualTo("Temperatura alta resolved by system")
    }

    private fun alert() = Alert(
        id = 1L,
        code = "ALT-00001",
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        sectorCode = null,
        alertTypeId = null,
        alertTypeName = null,
        severityId = 3,
        severityName = "ERROR",
        severityLevel = 3,
        message = "Temperatura alta",
        description = null,
        clientName = null,
        isResolved = true,
        resolvedAt = Instant.parse("2026-01-01T10:00:00Z"),
        resolvedByUserId = null,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T10:00:00Z")
    )

    private fun change() = AlertStateChange(
        id = null,
        alertId = 1L,
        fromResolved = false,
        toResolved = true,
        source = AlertSignalSource.API,
        rawValue = null,
        at = Instant.parse("2026-01-01T10:05:00Z"),
        actor = AlertActor.System
    )

    private fun severity() = NotificationSeveritySnapshot(
        id = 3,
        name = "ERROR",
        level = 3,
        color = "#FF0000",
        notifyPush = true
    )
}

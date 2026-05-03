package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationContent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationRecipient
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationContentRendererPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationSeveritySnapshot
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class MessageSourceContentRendererAdapter(
    @Qualifier("notificationMessageSource")
    private val messageSource: MessageSource
) : NotificationContentRendererPort {

    override fun render(
        type: NotificationType,
        alert: Alert,
        change: AlertStateChange?,
        recipient: NotificationRecipient,
        severity: NotificationSeveritySnapshot,
        agingContext: AlertAgingDetectedEvent?
    ): NotificationContent {
        val locale = recipient.locale
        val operatorMessage = alert.message ?: alert.description ?: alert.clientName ?: alert.code

        return when (type) {
            NotificationType.ALERT_ACTIVATED -> renderActivated(alert, severity, locale, operatorMessage)
            NotificationType.ALERT_RESOLVED -> renderResolved(alert, severity, change, locale, operatorMessage)
            NotificationType.ALERT_AGING -> renderAging(alert, severity, agingContext, locale, operatorMessage)
        }
    }

    private fun renderActivated(
        alert: Alert,
        severity: NotificationSeveritySnapshot,
        locale: Locale,
        operatorMessage: String
    ): NotificationContent {
        val severityLocalized = messageSource.getMessage(
            "notification.severity.${severity.name}",
            null,
            severity.name,
            locale
        )!!
        val title = messageSource.getMessage(
            "notification.alert.activated.title",
            arrayOf(severityLocalized),
            locale
        )!!

        return NotificationContent(
            title = title,
            body = operatorMessage,
            data = buildActivatedData(alert, severity),
            androidChannelId = NotificationType.ALERT_ACTIVATED.defaultChannelId,
            severityColor = severity.color
        )
    }

    private fun renderResolved(
        alert: Alert,
        severity: NotificationSeveritySnapshot,
        change: AlertStateChange?,
        locale: Locale,
        operatorMessage: String
    ): NotificationContent {
        val title = messageSource.getMessage(
            "notification.alert.resolved.title",
            arrayOf(alert.code),
            locale
        )!!

        val actorDescription = resolveActorDescription(change?.actor, locale)
        val body = messageSource.getMessage(
            "notification.alert.resolved.body",
            arrayOf(operatorMessage, actorDescription),
            locale
        )!!

        val dataMap = buildBaseData(alert, severity).toMutableMap()
        dataMap["notificationType"] = NotificationType.ALERT_RESOLVED.name
        appendActorData(change?.actor, dataMap)

        return NotificationContent(
            title = title,
            body = body,
            data = dataMap,
            androidChannelId = NotificationType.ALERT_RESOLVED.defaultChannelId,
            severityColor = severity.color
        )
    }

    private fun renderAging(
        alert: Alert,
        severity: NotificationSeveritySnapshot,
        agingContext: AlertAgingDetectedEvent?,
        locale: Locale,
        operatorMessage: String
    ): NotificationContent {
        val ageMinutes = agingContext?.ageMinutes ?: 0L
        val ageDescription = if (ageMinutes < 60) {
            messageSource.getMessage(
                "notification.aging.duration.minutes",
                arrayOf(ageMinutes),
                locale
            )!!
        } else {
            messageSource.getMessage(
                "notification.aging.duration.hours",
                arrayOf(ageMinutes / 60),
                locale
            )!!
        }

        val title = messageSource.getMessage(
            "notification.alert.aging.title",
            arrayOf<Any>(alert.code, ageMinutes),
            locale
        )!!
        val body = messageSource.getMessage(
            "notification.alert.aging.body",
            arrayOf(operatorMessage, ageDescription),
            locale
        )!!

        val dataMap = buildBaseData(alert, severity).toMutableMap()
        dataMap["notificationType"] = NotificationType.ALERT_AGING.name
        dataMap["ageMinutes"] = ageMinutes.toString()
        agingContext?.let { dataMap["activatedAt"] = it.activatedAt.toEpochMilli().toString() }

        return NotificationContent(
            title = title,
            body = body,
            data = dataMap,
            androidChannelId = NotificationType.ALERT_AGING.defaultChannelId,
            severityColor = severity.color
        )
    }

    private fun resolveActorDescription(actor: AlertActor?, locale: Locale): String = when (actor) {
        is AlertActor.User -> {
            val displayName = actor.displayName ?: actor.username ?: "user #${actor.userId}"
            messageSource.getMessage("notification.actor.user", arrayOf(displayName), locale)!!
        }
        is AlertActor.Device -> messageSource.getMessage("notification.actor.device", null, locale)!!
        is AlertActor.System, null -> messageSource.getMessage("notification.actor.system", null, locale)!!
    }

    private fun appendActorData(actor: AlertActor?, dataMap: MutableMap<String, String>) {
        when (actor) {
            is AlertActor.User -> {
                dataMap["actorKind"] = "USER"
                dataMap["actorUserId"] = actor.userId.toString()
            }
            is AlertActor.Device -> dataMap["actorKind"] = "DEVICE"
            is AlertActor.System, null -> dataMap["actorKind"] = "SYSTEM"
        }
    }

    private fun buildActivatedData(alert: Alert, severity: NotificationSeveritySnapshot): Map<String, String> =
        buildBaseData(alert, severity).toMutableMap().also {
            it["notificationType"] = NotificationType.ALERT_ACTIVATED.name
        }

    private fun buildBaseData(alert: Alert, severity: NotificationSeveritySnapshot): Map<String, String> {
        val data = mutableMapOf(
            "alertId" to (alert.id?.toString() ?: ""),
            "alertCode" to alert.code,
            "tenantId" to alert.tenantId.value.toString(),
            "sectorId" to alert.sectorId.value.toString(),
            "severity" to severity.name,
            "severityLevel" to severity.level.toString(),
            "createdAt" to alert.createdAt.toEpochMilli().toString()
        )
        return data
    }
}

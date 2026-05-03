package com.apptolast.invernaderos.features.notification.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.notification.domain.error.NotificationError
import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent
import com.apptolast.invernaderos.features.notification.domain.model.DropReason
import com.apptolast.invernaderos.features.notification.domain.model.NotificationContent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationLogEntry
import com.apptolast.invernaderos.features.notification.domain.model.NotificationRecipient
import com.apptolast.invernaderos.features.notification.domain.model.NotificationStatus
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.model.QuietHours
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.notification.domain.port.input.DispatchNotificationUseCase
import com.apptolast.invernaderos.features.notification.domain.port.input.DispatchSummary
import com.apptolast.invernaderos.features.notification.domain.port.output.AlertSeverityLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.FcmSenderPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationContentRendererPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationDedupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationLogRepositoryPort
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationSeveritySnapshot
import com.apptolast.invernaderos.features.notification.domain.port.output.PushTokenLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.UserLookupPort
import com.apptolast.invernaderos.features.notification.domain.port.output.UserPreferencesRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Orchestrates the end-to-end dispatch of a push notification for a given [NotificationType].
 *
 * Flow per token:
 * 1. Resolve severity via [AlertSeverityLookupPort]; if [notifyPush] is false return empty summary.
 * 2. Fetch all active tokens for the tenant via [PushTokenLookupPort].
 * 3. For each token: look up the user and their preferences, then evaluate per-user filters:
 *    - Category disabled  → DROPPED_BY_PREFERENCE
 *    - Severity too low   → DROPPED_BY_PREFERENCE
 *    - In quiet hours     → DROPPED_BY_QUIET_HOURS
 *    - Dedup hit          → DROPPED_BY_DEDUP
 * 4. Group passing recipients by locale, render per-locale content, send via [FcmSenderPort].
 * 5. Log every outcome (SENT, FAILED, TOKEN_INVALIDATED, or drop reason) via [NotificationLogRepositoryPort].
 * 6. Return a [DispatchSummary].
 *
 * [agingContext] must be passed when [type] is [NotificationType.ALERT_AGING]; the infrastructure
 * adapter that calls this use case is responsible for supplying the event that triggered the dispatch.
 */
class DispatchNotificationUseCaseImpl(
    private val alertSeverityLookup: AlertSeverityLookupPort,
    private val pushTokenLookup: PushTokenLookupPort,
    private val userLookup: UserLookupPort,
    private val preferencesRepository: UserPreferencesRepositoryPort,
    private val notificationDedupPort: NotificationDedupPort,
    private val contentRenderer: NotificationContentRendererPort,
    private val fcmSender: FcmSenderPort,
    private val notificationLogRepository: NotificationLogRepositoryPort,
    private val dedupWindowSeconds: Long = 60L
) : DispatchNotificationUseCase {

    override fun dispatch(
        type: NotificationType,
        alert: Alert,
        change: AlertStateChange?,
        agingContext: AlertAgingDetectedEvent?
    ): Either<NotificationError, DispatchSummary> {
        val severityId = alert.severityId
            ?: return Either.Right(DispatchSummary(sent = 0, dropped = 0, failed = 0))

        val severity = alertSeverityLookup.findById(severityId)
            ?: return Either.Left(NotificationError.AlertNotFound(alert.id ?: -1L))

        if (!severity.notifyPush) {
            return Either.Right(DispatchSummary(sent = 0, dropped = 0, failed = 0))
        }

        val alertId = alert.id
            ?: return Either.Left(NotificationError.AlertNotFound(-1L))

        val tenantId = alert.tenantId.value
        val tokens = pushTokenLookup.findActiveTokensForTenant(tenantId)

        if (tokens.isEmpty()) {
            return Either.Right(DispatchSummary(sent = 0, dropped = 0, failed = 0))
        }

        val now = Instant.now()
        val passingRecipients = mutableListOf<NotificationRecipient>()
        var droppedCount = 0

        for (token in tokens) {
            val user = userLookup.findById(token.userId) ?: continue

            val preferences = preferencesRepository.findByUserId(user.id)
                ?: UserNotificationPreferences.default(user.id)

            val dropReason = evaluateFilters(type, alert, preferences, now, alertId)

            if (dropReason != null) {
                logEntry(
                    tenantId = tenantId,
                    userId = user.id,
                    tokenId = token.id,
                    type = type,
                    status = dropReason.toNotificationStatus(),
                    payloadJson = buildMinimalPayloadJson(alert, type, severity),
                    fcmMessageId = null,
                    error = null
                )
                droppedCount++
                continue
            }

            val locale = Locale.forLanguageTag(user.locale)
            passingRecipients.add(
                NotificationRecipient(
                    userId = user.id,
                    tokenId = token.id,
                    tokenValue = token.token,
                    locale = locale
                )
            )
        }

        if (passingRecipients.isEmpty()) {
            return Either.Right(DispatchSummary(sent = 0, dropped = droppedCount, failed = 0))
        }

        val recipientsByLocale = passingRecipients.groupBy { it.locale }

        var totalSent = 0
        var totalFailed = 0

        for ((_, localeRecipients) in recipientsByLocale) {
            val representativeRecipient = localeRecipients.first()
            val content = contentRenderer.render(
                type = type,
                alert = alert,
                change = change,
                recipient = representativeRecipient,
                severity = severity,
                agingContext = agingContext
            )

            val sendResult = fcmSender.send(localeRecipients, content)

            for (recipient in localeRecipients) {
                when {
                    sendResult.invalidatedTokens.contains(recipient.tokenId) -> {
                        logEntry(
                            tenantId = tenantId,
                            userId = recipient.userId,
                            tokenId = recipient.tokenId,
                            type = type,
                            status = NotificationStatus.TOKEN_INVALIDATED,
                            payloadJson = buildPayloadJson(content),
                            fcmMessageId = null,
                            error = "Token permanently invalidated (UNREGISTERED or INVALID_ARGUMENT)"
                        )
                    }

                    sendResult.errors.containsKey(recipient.tokenId) -> {
                        logEntry(
                            tenantId = tenantId,
                            userId = recipient.userId,
                            tokenId = recipient.tokenId,
                            type = type,
                            status = NotificationStatus.FAILED,
                            payloadJson = buildPayloadJson(content),
                            fcmMessageId = null,
                            error = sendResult.errors[recipient.tokenId]
                        )
                        totalFailed++
                    }

                    else -> {
                        logEntry(
                            tenantId = tenantId,
                            userId = recipient.userId,
                            tokenId = recipient.tokenId,
                            type = type,
                            status = NotificationStatus.SENT,
                            payloadJson = buildPayloadJson(content),
                            fcmMessageId = null,
                            error = null
                        )
                        totalSent++
                    }
                }
            }
        }

        return Either.Right(DispatchSummary(sent = totalSent, dropped = droppedCount, failed = totalFailed))
    }

    private fun evaluateFilters(
        type: NotificationType,
        alert: Alert,
        preferences: UserNotificationPreferences,
        now: Instant,
        alertId: Long
    ): DropReason? {
        if (!preferences.categoryAlerts) return DropReason.CATEGORY_DISABLED

        val severityLevel = alert.severityLevel ?: 0
        if (severityLevel < preferences.minAlertSeverity) return DropReason.BELOW_MIN_SEVERITY

        if (preferences.quietHours.isWithin(now)) return DropReason.IN_QUIET_HOURS

        val dispatchAllowed = notificationDedupPort.shouldDispatch(
            type = type,
            alertId = alertId,
            userId = preferences.userId,
            windowSeconds = dedupWindowSeconds
        )
        if (!dispatchAllowed) return DropReason.DEDUP_HIT

        return null
    }

    private fun logEntry(
        tenantId: Long,
        userId: Long,
        tokenId: Long,
        type: NotificationType,
        status: NotificationStatus,
        payloadJson: String,
        fcmMessageId: String?,
        error: String?
    ) {
        notificationLogRepository.save(
            NotificationLogEntry(
                id = null,
                tenantId = tenantId,
                userId = userId,
                deviceTokenId = tokenId,
                notificationType = type,
                payloadJson = payloadJson,
                status = status,
                fcmMessageId = fcmMessageId,
                error = error,
                sentAt = Instant.now()
            )
        )
    }

    private fun buildMinimalPayloadJson(
        alert: Alert,
        type: NotificationType,
        severity: NotificationSeveritySnapshot
    ): String =
        """{"notificationType":"${type.name}","alertId":${alert.id},"alertCode":"${alert.code}","severity":"${severity.name}"}"""

    private fun buildPayloadJson(content: NotificationContent): String {
        val dataEntries = content.data.entries.joinToString(",") { (key, value) ->
            """"$key":"$value""""
        }
        return """{"title":"${content.title}","body":"${content.body}","data":{$dataEntries},"androidChannelId":"${content.androidChannelId}"}"""
    }
}

package com.apptolast.invernaderos.features.notification.dto.mapper

import com.apptolast.invernaderos.features.notification.domain.model.NotificationLogEntry
import com.apptolast.invernaderos.features.notification.domain.model.PreferredChannel
import com.apptolast.invernaderos.features.notification.domain.model.QuietHours
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationLogPage
import com.apptolast.invernaderos.features.notification.domain.model.UserNotificationPreferences
import com.apptolast.invernaderos.features.notification.dto.request.UpdateUserNotificationPreferencesRequest
import com.apptolast.invernaderos.features.notification.dto.response.UserNotificationLogEntryResponse
import com.apptolast.invernaderos.features.notification.dto.response.UserNotificationLogPageResponse
import com.apptolast.invernaderos.features.notification.dto.response.UserNotificationPreferencesResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalTime
import java.time.ZoneId

fun UserNotificationPreferences.toResponse(locale: String): UserNotificationPreferencesResponse =
    UserNotificationPreferencesResponse(
        categoryAlerts = categoryAlerts,
        categoryDevices = categoryDevices,
        categorySubscription = categorySubscription,
        minAlertSeverity = minAlertSeverity,
        quietHoursStart = quietHours.start?.toString(),
        quietHoursEnd = quietHours.end?.toString(),
        quietHoursTimezone = quietHours.timezone.id,
        preferredChannel = preferredChannel.name,
        locale = locale
    )

fun UpdateUserNotificationPreferencesRequest.toDomain(userId: Long): UserNotificationPreferences =
    UserNotificationPreferences(
        userId = userId,
        categoryAlerts = categoryAlerts,
        categoryDevices = categoryDevices,
        categorySubscription = categorySubscription,
        minAlertSeverity = minAlertSeverity,
        quietHours = QuietHours(
            start = quietHoursStart?.let { LocalTime.parse(it) },
            end = quietHoursEnd?.let { LocalTime.parse(it) },
            timezone = runCatching { ZoneId.of(quietHoursTimezone) }.getOrElse { ZoneId.of("Europe/Madrid") }
        ),
        preferredChannel = runCatching { PreferredChannel.valueOf(preferredChannel) }.getOrElse { PreferredChannel.PUSH }
    )

fun NotificationLogEntry.toResponse(objectMapper: ObjectMapper): UserNotificationLogEntryResponse {
    val payloadMap: Map<String, Any> = runCatching {
        objectMapper.readValue<Map<String, Any>>(payloadJson)
    }.getOrElse { emptyMap() }

    return UserNotificationLogEntryResponse(
        id = id ?: 0L,
        notificationType = notificationType.name,
        status = status.name,
        payload = payloadMap,
        fcmMessageId = fcmMessageId,
        error = error,
        sentAt = sentAt
    )
}

fun UserNotificationLogPage.toResponse(objectMapper: ObjectMapper): UserNotificationLogPageResponse =
    UserNotificationLogPageResponse(
        entries = entries.map { it.toResponse(objectMapper) },
        nextCursor = nextCursor
    )

package com.apptolast.invernaderos.features.notification.domain.model

/**
 * Immutable rendered notification payload ready to be handed to an FCM sender.
 *
 * [data] holds key-value pairs that the mobile client can read in the background.
 * [androidChannelId] maps to the Android notification channel that must be declared by the client.
 * [severityColor] is an optional hex string (e.g. "#FF0000") used to tint the notification icon.
 */
data class NotificationContent(
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val androidChannelId: String,
    val severityColor: String?
)

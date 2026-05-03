package com.apptolast.invernaderos.features.notification.dto.response

data class UserNotificationPreferencesResponse(
    val categoryAlerts: Boolean,
    val categoryDevices: Boolean,
    val categorySubscription: Boolean,
    val minAlertSeverity: Int,
    val quietHoursStart: String?,
    val quietHoursEnd: String?,
    val quietHoursTimezone: String,
    val preferredChannel: String,
    val locale: String
)

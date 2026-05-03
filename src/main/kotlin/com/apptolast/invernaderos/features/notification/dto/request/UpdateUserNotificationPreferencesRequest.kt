package com.apptolast.invernaderos.features.notification.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UpdateUserNotificationPreferencesRequest(
    val categoryAlerts: Boolean,
    val categoryDevices: Boolean,
    val categorySubscription: Boolean,

    @field:Min(1)
    @field:Max(4)
    val minAlertSeverity: Int,

    @field:Pattern(
        regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
        message = "quietHoursStart must be in HH:mm format"
    )
    val quietHoursStart: String? = null,

    @field:Pattern(
        regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
        message = "quietHoursEnd must be in HH:mm format"
    )
    val quietHoursEnd: String? = null,

    @field:NotBlank
    val quietHoursTimezone: String = "Europe/Madrid",

    @field:NotBlank
    val preferredChannel: String = "PUSH",

    @field:NotBlank
    @field:Pattern(
        regexp = "^[a-z]{2}-[A-Z]{2}$",
        message = "locale must be in the format 'xx-XX', e.g. 'es-ES'"
    )
    val locale: String = "es-ES"
)

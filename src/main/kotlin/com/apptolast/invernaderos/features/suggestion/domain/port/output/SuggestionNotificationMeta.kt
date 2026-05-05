package com.apptolast.invernaderos.features.suggestion.domain.port.output

import java.time.Instant

/**
 * Technical metadata attached to a notification. Lives in the output port package because
 * it is only meaningful at the notification boundary — the [com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion]
 * entity itself stays minimal (category, title, description).
 */
data class SuggestionNotificationMeta(
    val appVersion: String,
    val platform: String,
    val userEmail: String,
    val submittedAt: Instant
)

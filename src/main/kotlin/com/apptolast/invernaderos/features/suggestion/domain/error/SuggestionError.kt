package com.apptolast.invernaderos.features.suggestion.domain.error

sealed interface SuggestionError {
    val message: String

    data class IssueCreationFailed(val cause: String) : SuggestionError {
        override val message: String
            get() = "Failed to create GitHub issue: $cause"
    }

    data class NotificationFailed(val cause: String) : SuggestionError {
        override val message: String
            get() = "Failed to send notification email: $cause"
    }
}

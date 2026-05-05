package com.apptolast.invernaderos.features.suggestion.domain.port.input

import com.apptolast.invernaderos.features.suggestion.domain.error.SuggestionError
import com.apptolast.invernaderos.features.suggestion.domain.model.GitHubIssueRef

/**
 * Three-valued outcome of submitting a suggestion.
 *
 * - [Success] both issue and email succeeded.
 * - [PartialSuccess] issue created but email delivery failed; the suggestion is still
 *   recorded (the issue is the source of truth) and the controller returns 201 + warning.
 * - [Failure] issue creation itself failed; nothing was recorded and the controller
 *   returns 502 so the user knows to retry.
 */
sealed interface SubmitSuggestionResult {
    data class Success(val issueRef: GitHubIssueRef) : SubmitSuggestionResult

    data class PartialSuccess(
        val issueRef: GitHubIssueRef,
        val notificationError: SuggestionError.NotificationFailed
    ) : SubmitSuggestionResult

    data class Failure(val error: SuggestionError.IssueCreationFailed) : SubmitSuggestionResult
}

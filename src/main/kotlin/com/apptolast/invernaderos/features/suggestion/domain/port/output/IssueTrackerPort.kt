package com.apptolast.invernaderos.features.suggestion.domain.port.output

import com.apptolast.invernaderos.features.suggestion.domain.model.GitHubIssueRef
import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion

/**
 * Driven port for creating an issue in an external issue tracker (GitHub in production).
 *
 * Implementations may throw on transport / authentication / 4xx-5xx errors. The use case
 * catches and converts the exception into a [com.apptolast.invernaderos.features.suggestion.application.usecase.SubmitSuggestionResult.Failure].
 */
interface IssueTrackerPort {
    fun createIssue(suggestion: Suggestion): GitHubIssueRef
}

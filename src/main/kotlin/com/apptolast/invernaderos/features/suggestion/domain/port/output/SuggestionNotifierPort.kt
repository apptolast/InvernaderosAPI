package com.apptolast.invernaderos.features.suggestion.domain.port.output

import com.apptolast.invernaderos.features.suggestion.domain.model.GitHubIssueRef
import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion

/**
 * Driven port for notifying the development team about a newly submitted suggestion.
 *
 * Carries [SuggestionNotificationMeta] which contains the technical metadata that lives
 * outside of the domain entity (app version, platform, user email, submission timestamp,
 * link to the created issue). Implementations may throw on transport errors; the use
 * case treats notification failure as a partial success.
 */
interface SuggestionNotifierPort {
    fun notify(
        suggestion: Suggestion,
        meta: SuggestionNotificationMeta,
        issueRef: GitHubIssueRef
    )
}

package com.apptolast.invernaderos.features.suggestion.application.usecase

import com.apptolast.invernaderos.features.suggestion.domain.error.SuggestionError
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionCommand
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionResult
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionUseCase
import com.apptolast.invernaderos.features.suggestion.domain.port.output.IssueTrackerPort
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotifierPort
import org.slf4j.LoggerFactory

/**
 * Orchestrates the two-step submission: (1) create the GitHub issue (source of truth),
 * (2) notify the team by email (best-effort).
 *
 * If issue creation fails the whole operation fails (502 to caller). If only the email
 * fails the operation is a partial success — the suggestion is recorded in GitHub and
 * the caller gets a 201 with a warning.
 */
class SubmitSuggestionUseCaseImpl(
    private val issueTracker: IssueTrackerPort,
    private val notifier: SuggestionNotifierPort
) : SubmitSuggestionUseCase {

    private val logger = LoggerFactory.getLogger(SubmitSuggestionUseCaseImpl::class.java)

    override fun execute(command: SubmitSuggestionCommand): SubmitSuggestionResult {
        val issueRef = try {
            issueTracker.createIssue(command.suggestion)
        } catch (ex: Exception) {
            logger.error(
                "Suggestion submission failed at issue creation step (category='{}'): {}",
                command.suggestion.category.value, ex.message, ex
            )
            return SubmitSuggestionResult.Failure(
                SuggestionError.IssueCreationFailed(ex.message ?: ex.javaClass.simpleName)
            )
        }

        return try {
            notifier.notify(command.suggestion, command.meta, issueRef)
            logger.info(
                "Suggestion submitted successfully: issue #{} ({})",
                issueRef.number, issueRef.htmlUrl
            )
            SubmitSuggestionResult.Success(issueRef)
        } catch (ex: Exception) {
            logger.warn(
                "Suggestion issue #{} created but email notification failed: {}",
                issueRef.number, ex.message, ex
            )
            SubmitSuggestionResult.PartialSuccess(
                issueRef = issueRef,
                notificationError = SuggestionError.NotificationFailed(
                    ex.message ?: ex.javaClass.simpleName
                )
            )
        }
    }
}

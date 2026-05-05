package com.apptolast.invernaderos.features.suggestion.application.usecase

import com.apptolast.invernaderos.features.suggestion.domain.model.GitHubIssueRef
import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionCategory
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionDescription
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionTitle
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionCommand
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionResult
import com.apptolast.invernaderos.features.suggestion.domain.port.output.IssueTrackerPort
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotificationMeta
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotifierPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubmitSuggestionUseCaseImplTest {

    private val issueTracker = mockk<IssueTrackerPort>()
    private val notifier = mockk<SuggestionNotifierPort>(relaxed = true)
    private val useCase = SubmitSuggestionUseCaseImpl(issueTracker, notifier)

    private val suggestion = Suggestion(
        category = SuggestionCategory("Sugerencia"),
        title = SuggestionTitle("Mejorar el filtro"),
        description = SuggestionDescription("Sería útil filtrar por sector y severidad")
    )
    private val meta = SuggestionNotificationMeta(
        appVersion = "0.2.0",
        platform = "Android 36",
        userEmail = "user@example.com",
        submittedAt = Instant.parse("2026-05-05T09:33:17Z")
    )
    private val command = SubmitSuggestionCommand(suggestion, meta)
    private val issueRef = GitHubIssueRef(number = 42, htmlUrl = "https://github.com/apptolast/GreenhouseFronts/issues/42")

    @Test
    fun `Success path returns Success and notifies`() {
        every { issueTracker.createIssue(suggestion) } returns issueRef

        val result = useCase.execute(command)

        val success = assertIs<SubmitSuggestionResult.Success>(result)
        assertEquals(issueRef, success.issueRef)
        verify(exactly = 1) { issueTracker.createIssue(suggestion) }
        verify(exactly = 1) { notifier.notify(suggestion, meta, issueRef) }
    }

    @Test
    fun `When email fails returns PartialSuccess and keeps the issue ref`() {
        every { issueTracker.createIssue(suggestion) } returns issueRef
        every { notifier.notify(any(), any(), any()) } throws RuntimeException("smtp down")

        val result = useCase.execute(command)

        val partial = assertIs<SubmitSuggestionResult.PartialSuccess>(result)
        assertEquals(issueRef, partial.issueRef)
        assertTrue(partial.notificationError.message.contains("smtp down"))
    }

    @Test
    fun `When issue creation fails returns Failure and never notifies`() {
        every { issueTracker.createIssue(suggestion) } throws RuntimeException("github 502")

        val result = useCase.execute(command)

        val failure = assertIs<SubmitSuggestionResult.Failure>(result)
        assertTrue(failure.error.message.contains("github 502"))
        verify(exactly = 0) { notifier.notify(any(), any(), any()) }
    }
}

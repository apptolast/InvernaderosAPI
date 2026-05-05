package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.email

import com.apptolast.invernaderos.features.suggestion.domain.model.GitHubIssueRef
import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionCategory
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionDescription
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionTitle
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotificationMeta
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mail.javamail.JavaMailSender
import java.time.Instant
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmtpSuggestionNotifierAdapterTest {

    private val session: Session = Session.getInstance(Properties())
    private val mailSender = mockk<JavaMailSender>()
    private val properties = SuggestionEmailProperties(
        from = "admin@apptolast.com",
        subjectPrefix = "[Kropia]",
        recipients = listOf("admin@apptolast.com", "hgarcia.alberto@gmail.com")
    )
    private val adapter = SmtpSuggestionNotifierAdapter(mailSender, properties)

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
    private val issueRef = GitHubIssueRef(
        number = 42,
        htmlUrl = "https://github.com/apptolast/GreenhouseFronts/issues/42"
    )

    @Test
    fun `notify populates From Subject TO and CC and delegates to mailSender`() {
        val mime = MimeMessage(session)
        every { mailSender.createMimeMessage() } returns mime
        val sent = slot<MimeMessage>()
        every { mailSender.send(capture(sent)) } returns Unit

        adapter.notify(suggestion, meta, issueRef)

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
        val captured = sent.captured
        assertEquals("[Kropia][Sugerencia] Mejorar el filtro", captured.subject)
        assertEquals("admin@apptolast.com", captured.from.single().toString())
        assertEquals(
            listOf("admin@apptolast.com", "hgarcia.alberto@gmail.com"),
            captured.getRecipients(Message.RecipientType.TO).map { it.toString() }
        )
        assertEquals(
            listOf("user@example.com"),
            captured.getRecipients(Message.RecipientType.CC).map { it.toString() }
        )
    }

    @Test
    fun `notify fails fast when recipient list is empty`() {
        val emptyAdapter = SmtpSuggestionNotifierAdapter(
            mailSender,
            SuggestionEmailProperties(from = "x@y.com", subjectPrefix = "[Kropia]", recipients = emptyList())
        )

        val ex = assertThrows<IllegalArgumentException> {
            emptyAdapter.notify(suggestion, meta, issueRef)
        }
        assertTrue(ex.message!!.contains("recipients"))
    }

    @Test
    fun `buildSubject formats prefix category and title`() {
        assertEquals("[Kropia][Sugerencia] Mejorar el filtro", adapter.buildSubject(suggestion))
    }

    @Test
    fun `buildHtmlBody contains all relevant fields and the issue link`() {
        val html = adapter.buildHtmlBody(suggestion, meta, issueRef)
        assertTrue(html.contains("Mejorar el filtro"))
        assertTrue(html.contains("Ser&iacute;a") || html.contains("Sería"))  // either escaped or raw
        assertTrue(html.contains("Kropia v0.2.0"))
        assertTrue(html.contains("Android 36"))
        assertTrue(html.contains("user@example.com"))
        assertTrue(html.contains(issueRef.htmlUrl))
        assertTrue(html.contains("#42"))
    }

    @Test
    fun `buildHtmlBody escapes HTML special characters in user input`() {
        val maliciousSuggestion = Suggestion(
            category = SuggestionCategory("<script>"),
            title = SuggestionTitle("\"injection&attack\""),
            description = SuggestionDescription("body <b>x</b>")
        )
        val html = adapter.buildHtmlBody(maliciousSuggestion, meta, issueRef)
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&quot;injection&amp;attack&quot;"))
        assertTrue(html.contains("body &lt;b&gt;x&lt;/b&gt;"))
        assertTrue(!html.contains("<script>"))
    }
}

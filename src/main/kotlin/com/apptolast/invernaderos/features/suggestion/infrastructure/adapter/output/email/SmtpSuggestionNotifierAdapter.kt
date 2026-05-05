package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.email

import com.apptolast.invernaderos.features.suggestion.domain.model.GitHubIssueRef
import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotificationMeta
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotifierPort
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import java.time.format.DateTimeFormatter

/**
 * Sends the suggestion notification via the SMTP transport already configured in
 * `spring.mail.*` (Gmail in dev/prod). The team recipients are in the TO field
 * (configured per environment) and the submitting user is added in CC so they
 * get a copy.
 */
class SmtpSuggestionNotifierAdapter(
    private val mailSender: JavaMailSender,
    private val properties: SuggestionEmailProperties
) : SuggestionNotifierPort {

    private val logger = LoggerFactory.getLogger(SmtpSuggestionNotifierAdapter::class.java)

    override fun notify(
        suggestion: Suggestion,
        meta: SuggestionNotificationMeta,
        issueRef: GitHubIssueRef
    ) {
        require(properties.recipients.isNotEmpty()) {
            "suggestion.email.recipients is empty (configure SUGGESTION_EMAIL_RECIPIENTS or the property list)"
        }

        val mime = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mime, true, "UTF-8")
        helper.setFrom(properties.from)
        helper.setTo(properties.recipients.toTypedArray())
        helper.setCc(meta.userEmail)
        helper.setSubject(buildSubject(suggestion))
        helper.setText(buildHtmlBody(suggestion, meta, issueRef), true)

        mailSender.send(mime)
        logger.info(
            "Suggestion email sent for issue #{} to {} recipients (CC: {})",
            issueRef.number, properties.recipients.size, meta.userEmail
        )
    }

    internal fun buildSubject(suggestion: Suggestion): String =
        "${properties.subjectPrefix}[${suggestion.category.value}] ${suggestion.title.value}"

    internal fun buildHtmlBody(
        suggestion: Suggestion,
        meta: SuggestionNotificationMeta,
        issueRef: GitHubIssueRef
    ): String {
        val submittedAtIso = DateTimeFormatter.ISO_INSTANT.format(meta.submittedAt)
        return """
            <html>
              <body style="font-family: -apple-system, Segoe UI, Roboto, sans-serif; color: #1f2328;">
                <h2 style="margin-bottom: 4px;">Nueva sugerencia recibida</h2>
                <p style="margin-top: 0; color: #57606a;">Issue creada en GitHub: <a href="${escape(issueRef.htmlUrl)}">#${issueRef.number}</a></p>

                <table style="border-collapse: collapse; margin-top: 16px;">
                  <tr>
                    <td style="padding: 6px 12px 6px 0; font-weight: 600;">Categoría</td>
                    <td style="padding: 6px 0;">${escape(suggestion.category.value)}</td>
                  </tr>
                  <tr>
                    <td style="padding: 6px 12px 6px 0; font-weight: 600;">Título</td>
                    <td style="padding: 6px 0;">${escape(suggestion.title.value)}</td>
                  </tr>
                </table>

                <h3 style="margin-top: 24px; margin-bottom: 4px;">Descripción</h3>
                <pre style="white-space: pre-wrap; background: #f6f8fa; border: 1px solid #d0d7de; border-radius: 6px; padding: 12px; font-family: inherit;">${escape(suggestion.description.value)}</pre>

                <h3 style="margin-top: 24px; margin-bottom: 4px;">Información técnica añadida automáticamente</h3>
                <table style="border-collapse: collapse;">
                  <tr><td style="padding: 4px 12px 4px 0; color: #57606a;">App</td><td style="padding: 4px 0;">Kropia v${escape(meta.appVersion)}</td></tr>
                  <tr><td style="padding: 4px 12px 4px 0; color: #57606a;">Plataforma</td><td style="padding: 4px 0;">${escape(meta.platform)}</td></tr>
                  <tr><td style="padding: 4px 12px 4px 0; color: #57606a;">Usuario</td><td style="padding: 4px 0;">${escape(meta.userEmail)}</td></tr>
                  <tr><td style="padding: 4px 12px 4px 0; color: #57606a;">Email</td><td style="padding: 4px 0;">${escape(meta.userEmail)}</td></tr>
                  <tr><td style="padding: 4px 12px 4px 0; color: #57606a;">Fecha</td><td style="padding: 4px 0;">$submittedAtIso</td></tr>
                </table>
              </body>
            </html>
        """.trimIndent()
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}

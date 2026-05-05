package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.email

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "suggestion.email")
data class SuggestionEmailProperties(
    /** Address used as the email From: header. Must be a Gmail account authenticated by SMTP. */
    val from: String = "admin@apptolast.com",

    /** Subject prefix prepended to every email. */
    val subjectPrefix: String = "[Kropia]",

    /** Team recipients (TO field). Configurable per environment. */
    val recipients: List<String> = emptyList()
)

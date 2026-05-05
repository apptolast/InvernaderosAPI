package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.github

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "suggestion.github")
data class GitHubProperties(
    /** GitHub App ID (numeric, shown on the App settings page). */
    val appId: String = "",

    /** Installation ID for the apptolast org (numeric, in the URL after Install). */
    val installationId: String = "",

    /**
     * Full PEM private key of the GitHub App in PKCS#8 format (header
     * `-----BEGIN PRIVATE KEY-----`). If GitHub gave you a PKCS#1 key
     * (`-----BEGIN RSA PRIVATE KEY-----`), convert it once before applying
     * the Secret:
     * `openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in app.pem -out app-pkcs8.pem`
     */
    val privateKey: String = "",

    /** Owner of the target repository (apptolast). */
    val owner: String = "apptolast",

    /** Target repository to create issues in. */
    val repo: String = "GreenhouseFronts",

    /** Base URL of the GitHub API (overridable in tests). */
    val apiBaseUrl: String = "https://api.github.com",

    /** Per-call HTTP timeout. */
    val requestTimeout: Duration = Duration.ofSeconds(10)
)

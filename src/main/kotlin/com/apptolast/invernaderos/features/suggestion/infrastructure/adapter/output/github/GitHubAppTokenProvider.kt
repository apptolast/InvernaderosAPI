package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.github

import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.concurrent.atomic.AtomicReference

/**
 * Issues short-lived installation access tokens for the GitHub App.
 *
 * Flow:
 * 1. Build a 9-minute JWT signed with the App private key (RS256), `iss = appId`.
 * 2. POST to `/app/installations/{id}/access_tokens` to exchange that JWT for an
 *    installation token (~60 min lifetime, returned by GitHub).
 * 3. Cache the token in memory and reuse it until 5 minutes before expiry.
 *
 * Thread-safe via AtomicReference. Failures bubble up as exceptions; the calling
 * adapter converts them into [com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionResult.Failure].
 */
class GitHubAppTokenProvider(
    private val properties: GitHubProperties,
    private val restClient: RestClient
) {

    private val logger = LoggerFactory.getLogger(GitHubAppTokenProvider::class.java)
    private val cachedToken = AtomicReference<CachedToken?>(null)
    private val privateKey: PrivateKey by lazy { parsePrivateKey(properties.privateKey) }

    /** Returns a valid installation token, refreshing if cached one is stale or absent. */
    fun getInstallationToken(): String {
        val cached = cachedToken.get()
        if (cached != null && cached.isStillValid()) {
            return cached.token
        }
        val refreshed = requestNewInstallationToken()
        cachedToken.set(refreshed)
        return refreshed.token
    }

    private fun requestNewInstallationToken(): CachedToken {
        require(properties.appId.isNotBlank()) {
            "suggestion.github.app-id is not configured (set GITHUB_APP_ID)"
        }
        require(properties.installationId.isNotBlank()) {
            "suggestion.github.installation-id is not configured (set GITHUB_APP_INSTALLATION_ID)"
        }
        require(properties.privateKey.isNotBlank()) {
            "suggestion.github.private-key is not configured (set GITHUB_APP_PRIVATE_KEY)"
        }

        val appJwt = buildAppJwt()
        val url = "${properties.apiBaseUrl.trimEnd('/')}/app/installations/${properties.installationId}/access_tokens"

        logger.debug("Requesting new GitHub installation token from {}", url)

        val response = restClient.post()
            .uri(url)
            .header("Authorization", "Bearer $appJwt")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "InvernaderosAPI-SuggestionBot")
            .retrieve()
            .body(InstallationTokenResponse::class.java)
            ?: throw IllegalStateException("GitHub returned an empty installation-token response")

        val expiresAt = Instant.parse(response.expires_at)
        logger.info("GitHub installation token refreshed (expires at {})", expiresAt)
        return CachedToken(response.token, expiresAt)
    }

    private fun buildAppJwt(): String {
        val now = Instant.now()
        // GitHub allows up to 10 min; clock skew tolerance subtracts 30s on iat.
        val issuedAt = now.minusSeconds(30)
        val expiresAt = now.plus(Duration.ofMinutes(9))
        return Jwts.builder()
            .issuer(properties.appId)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact()
    }

    private fun parsePrivateKey(pem: String): PrivateKey {
        val cleaned = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        require(cleaned.isNotEmpty()) {
            "GitHub App private key is empty. Expected PKCS#8 PEM (header '-----BEGIN PRIVATE KEY-----')."
        }
        val keyBytes = try {
            Base64.getDecoder().decode(cleaned)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException(
                "GitHub App private key is not valid base64. Make sure it is a PKCS#8 PEM. " +
                    "If GitHub gave you '-----BEGIN RSA PRIVATE KEY-----' (PKCS#1), convert with: " +
                    "openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in app.pem -out app-pkcs8.pem",
                ex
            )
        }
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    private data class CachedToken(val token: String, val expiresAt: Instant) {
        fun isStillValid(): Boolean =
            Instant.now().isBefore(expiresAt.minusSeconds(REFRESH_MARGIN_SECONDS))

        companion object {
            const val REFRESH_MARGIN_SECONDS = 300L  // 5 min
        }
    }

    /** Shape returned by `POST /app/installations/{id}/access_tokens`. */
    @Suppress("ConstructorParameterNaming", "PropertyName")
    internal data class InstallationTokenResponse(
        val token: String = "",
        val expires_at: String = ""
    )
}

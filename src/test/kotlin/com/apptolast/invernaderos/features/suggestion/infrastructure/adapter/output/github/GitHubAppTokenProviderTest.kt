package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.github

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import kotlin.test.assertEquals

class GitHubAppTokenProviderTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var restClient: RestClient
    private lateinit var properties: GitHubProperties
    private lateinit var provider: GitHubAppTokenProvider

    @BeforeEach
    fun setUp() {
        val kpg = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val keyPair = kpg.generateKeyPair()
        val pem = "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded) +
            "\n-----END PRIVATE KEY-----"

        properties = GitHubProperties(
            appId = "3608624",
            installationId = "999",
            privateKey = pem,
            owner = "apptolast",
            repo = "GreenhouseFronts",
            apiBaseUrl = "https://api.github.com"
        )

        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        restClient = builder.build()
        provider = GitHubAppTokenProvider(properties, restClient)
    }

    @Test
    fun `first call requests an installation token and second call serves it from cache`() {
        val futureExpiry = Instant.now().plusSeconds(3600).toString()
        server.expect(
            ExpectedCount.once(),
            requestTo("https://api.github.com/app/installations/999/access_tokens")
        )
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"token":"ghs_TESTTOKEN","expires_at":"$futureExpiry"}""",
                    MediaType.APPLICATION_JSON
                )
            )

        val first = provider.getInstallationToken()
        val second = provider.getInstallationToken()

        assertEquals("ghs_TESTTOKEN", first)
        assertEquals("ghs_TESTTOKEN", second)
        server.verify()  // confirms only ONE HTTP call was made
    }

    @Test
    fun `token close to expiry triggers a refresh`() {
        // First response: token expires in 4 minutes (< 5 min refresh margin) → next call refreshes.
        val nearExpiry = Instant.now().plusSeconds(240).toString()
        val freshExpiry = Instant.now().plusSeconds(3600).toString()
        server.expect(ExpectedCount.once(), requestTo("https://api.github.com/app/installations/999/access_tokens"))
            .andRespond(withSuccess("""{"token":"old","expires_at":"$nearExpiry"}""", MediaType.APPLICATION_JSON))
        server.expect(ExpectedCount.once(), requestTo("https://api.github.com/app/installations/999/access_tokens"))
            .andRespond(withSuccess("""{"token":"fresh","expires_at":"$freshExpiry"}""", MediaType.APPLICATION_JSON))

        val first = provider.getInstallationToken()
        val second = provider.getInstallationToken()

        assertEquals("old", first)
        assertEquals("fresh", second)
        server.verify()
    }
}

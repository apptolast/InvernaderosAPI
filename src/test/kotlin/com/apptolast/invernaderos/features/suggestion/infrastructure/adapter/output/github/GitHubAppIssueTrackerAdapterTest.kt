package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.github

import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionCategory
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionDescription
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionTitle
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.http.HttpMethod
import kotlin.test.assertEquals

class GitHubAppIssueTrackerAdapterTest {

    private val properties = GitHubProperties(
        appId = "123",
        installationId = "456",
        privateKey = "stub",
        owner = "apptolast",
        repo = "GreenhouseFronts",
        apiBaseUrl = "https://api.github.com"
    )
    private val tokenProvider = mockk<GitHubAppTokenProvider>()

    private val builder = RestClient.builder()
    private val server: MockRestServiceServer = MockRestServiceServer.bindTo(builder).build()
    private val restClient = builder.build()

    private val adapter = GitHubAppIssueTrackerAdapter(properties, tokenProvider, restClient)

    private val suggestion = Suggestion(
        category = SuggestionCategory("Sugerencia"),
        title = SuggestionTitle("Mejorar el filtro"),
        description = SuggestionDescription("Sería útil filtrar por sector y severidad")
    )

    @Test
    fun `createIssue posts to repos endpoint with bearer token and returns parsed ref`() {
        every { tokenProvider.getInstallationToken() } returns "ghs_installtoken"

        server.expect(requestTo("https://api.github.com/repos/apptolast/GreenhouseFronts/issues"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer ghs_installtoken"))
            .andExpect(header("Accept", "application/vnd.github+json"))
            .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
            .andExpect(jsonPath("$.title").value("Mejorar el filtro"))
            .andExpect(jsonPath("$.body").value("**Categoría:** Sugerencia\n\nSería útil filtrar por sector y severidad"))
            .andRespond(
                withSuccess(
                    """{"number":42,"html_url":"https://github.com/apptolast/GreenhouseFronts/issues/42"}""",
                    MediaType.APPLICATION_JSON
                )
            )

        val ref = adapter.createIssue(suggestion)

        assertEquals(42, ref.number)
        assertEquals("https://github.com/apptolast/GreenhouseFronts/issues/42", ref.htmlUrl)
        server.verify()
    }

    @Test
    fun `createIssue throws when GitHub responds non-2xx`() {
        every { tokenProvider.getInstallationToken() } returns "ghs_token"

        server.expect(requestTo("https://api.github.com/repos/apptolast/GreenhouseFronts/issues"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("""{"message":"Bad credentials"}"""))

        val ex = assertThrows<IllegalStateException> { adapter.createIssue(suggestion) }
        assert(ex.message!!.contains("401")) { "expected 401 in message: ${ex.message}" }
    }
}

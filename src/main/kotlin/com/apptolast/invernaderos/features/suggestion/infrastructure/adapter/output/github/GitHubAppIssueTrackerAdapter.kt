package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.github

import com.apptolast.invernaderos.features.suggestion.domain.model.GitHubIssueRef
import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion
import com.apptolast.invernaderos.features.suggestion.domain.port.output.IssueTrackerPort
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * Creates an issue in `{owner}/{repo}` using a fresh GitHub App installation token
 * obtained via [GitHubAppTokenProvider].
 *
 * Maps the GitHub response to [GitHubIssueRef]. Any non-201 response or transport
 * failure throws — the use case treats it as [com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionResult.Failure].
 */
class GitHubAppIssueTrackerAdapter(
    private val properties: GitHubProperties,
    private val tokenProvider: GitHubAppTokenProvider,
    private val restClient: RestClient
) : IssueTrackerPort {

    private val logger = LoggerFactory.getLogger(GitHubAppIssueTrackerAdapter::class.java)

    override fun createIssue(suggestion: Suggestion): GitHubIssueRef {
        val token = tokenProvider.getInstallationToken()
        val url = "${properties.apiBaseUrl.trimEnd('/')}/repos/${properties.owner}/${properties.repo}/issues"

        val body = CreateIssueBody(
            title = suggestion.title.value,
            body = buildIssueBody(suggestion)
        )

        logger.debug("Creating GitHub issue at {} (title='{}')", url, body.title)

        val response = try {
            restClient.post()
                .uri(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "InvernaderosAPI-SuggestionBot")
                .body(body)
                .retrieve()
                .body(CreateIssueResponse::class.java)
                ?: throw IllegalStateException("GitHub returned an empty issue-creation response")
        } catch (ex: RestClientResponseException) {
            throw IllegalStateException(
                "GitHub returned ${ex.statusCode.value()}: ${ex.responseBodyAsString.take(500)}",
                ex
            )
        }

        if (response.html_url.isBlank() || response.number == 0) {
            throw IllegalStateException("GitHub response missing html_url or number: $response")
        }
        return GitHubIssueRef(number = response.number, htmlUrl = response.html_url)
    }

    private fun buildIssueBody(suggestion: Suggestion): String =
        "**Categoría:** ${suggestion.category.value}\n\n${suggestion.description.value}"

    @Suppress("ConstructorParameterNaming")
    internal data class CreateIssueBody(val title: String, val body: String)

    @Suppress("ConstructorParameterNaming", "PropertyName")
    internal data class CreateIssueResponse(
        val number: Int = 0,
        val html_url: String = ""
    )
}

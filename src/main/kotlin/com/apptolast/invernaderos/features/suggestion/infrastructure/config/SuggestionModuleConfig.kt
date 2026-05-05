package com.apptolast.invernaderos.features.suggestion.infrastructure.config

import com.apptolast.invernaderos.features.suggestion.application.usecase.SubmitSuggestionUseCaseImpl
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionUseCase
import com.apptolast.invernaderos.features.suggestion.domain.port.output.IssueTrackerPort
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotifierPort
import com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.email.SmtpSuggestionNotifierAdapter
import com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.email.SuggestionEmailProperties
import com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.github.GitHubAppIssueTrackerAdapter
import com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.github.GitHubAppTokenProvider
import com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.output.github.GitHubProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.web.client.RestClient
import java.time.Clock

@Configuration
@EnableConfigurationProperties(GitHubProperties::class, SuggestionEmailProperties::class)
class SuggestionModuleConfig {

    @Bean
    fun suggestionRestClient(): RestClient = RestClient.builder().build()

    @Bean
    fun suggestionClock(): Clock = Clock.systemUTC()

    @Bean
    fun gitHubAppTokenProvider(
        properties: GitHubProperties,
        restClient: RestClient
    ): GitHubAppTokenProvider = GitHubAppTokenProvider(properties, restClient)

    @Bean
    fun issueTrackerPort(
        properties: GitHubProperties,
        tokenProvider: GitHubAppTokenProvider,
        restClient: RestClient
    ): IssueTrackerPort = GitHubAppIssueTrackerAdapter(properties, tokenProvider, restClient)

    @Bean
    fun suggestionNotifierPort(
        mailSender: JavaMailSender,
        properties: SuggestionEmailProperties
    ): SuggestionNotifierPort = SmtpSuggestionNotifierAdapter(mailSender, properties)

    @Bean
    fun submitSuggestionUseCase(
        issueTracker: IssueTrackerPort,
        notifier: SuggestionNotifierPort
    ): SubmitSuggestionUseCase = SubmitSuggestionUseCaseImpl(issueTracker, notifier)
}

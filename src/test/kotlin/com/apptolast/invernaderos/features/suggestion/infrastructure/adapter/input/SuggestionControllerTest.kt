package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.input

import com.apptolast.invernaderos.core.exception.GlobalExceptionHandler
import com.apptolast.invernaderos.features.suggestion.domain.error.SuggestionError
import com.apptolast.invernaderos.features.suggestion.domain.model.GitHubIssueRef
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionResult
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SuggestionControllerTest {

    private val useCase = mockk<SubmitSuggestionUseCase>()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-05T09:33:17Z"), ZoneOffset.UTC)
    private val controller = SuggestionController(useCase, clock)

    // Use Spring's Jackson2ObjectMapperBuilder so the resulting ObjectMapper has the
    // same defaults as Spring Boot in production: KotlinModule, JavaTimeModule, plus
    // the @JsonAnyGetter handling that ProblemDetail relies on to expose its custom
    // properties (errorCode, errorId, etc.) at the JSON root.
    private val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    private val validBody = """
        {
          "category": "Sugerencia",
          "title": "Mejorar el filtro",
          "description": "Sería útil filtrar por sector y severidad",
          "appVersion": "0.2.0",
          "platform": "Android 36",
          "userEmail": "user@example.com"
        }
    """.trimIndent()

    @Test
    fun `Success returns 201 with issue url and emailSent true`() {
        every { useCase.execute(any()) } returns SubmitSuggestionResult.Success(
            GitHubIssueRef(42, "https://github.com/apptolast/GreenhouseFronts/issues/42")
        )

        mockMvc.perform(
            post("/api/v1/suggestions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.issueNumber").value(42))
            .andExpect(jsonPath("$.issueUrl").value("https://github.com/apptolast/GreenhouseFronts/issues/42"))
            .andExpect(jsonPath("$.emailSent").value(true))
            .andExpect(jsonPath("$.warning").doesNotExist())
    }

    @Test
    fun `PartialSuccess returns 201 with emailSent false and warning text`() {
        every { useCase.execute(any()) } returns SubmitSuggestionResult.PartialSuccess(
            GitHubIssueRef(43, "https://github.com/apptolast/GreenhouseFronts/issues/43"),
            SuggestionError.NotificationFailed("smtp down")
        )

        mockMvc.perform(
            post("/api/v1/suggestions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.issueNumber").value(43))
            .andExpect(jsonPath("$.emailSent").value(false))
            .andExpect(jsonPath("$.warning").exists())
    }

    @Test
    fun `Failure returns 502 with ProblemDetail and ISSUE_TRACKER_UNAVAILABLE error code`() {
        every { useCase.execute(any()) } returns SubmitSuggestionResult.Failure(
            SuggestionError.IssueCreationFailed("github 503")
        )

        mockMvc.perform(
            post("/api/v1/suggestions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        )
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.errorCode").value("ISSUE_TRACKER_UNAVAILABLE"))
            .andExpect(jsonPath("$.title").value("Issue Tracker Unavailable"))
    }

    @Test
    fun `Validation error on blank title returns 400`() {
        val invalidBody = """
            {
              "category": "Sugerencia",
              "title": "",
              "description": "ok desc",
              "appVersion": "0.2.0",
              "platform": "Android 36",
              "userEmail": "user@example.com"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/suggestions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }

    @Test
    fun `Validation error on invalid email returns 400`() {
        val invalidBody = """
            {
              "category": "Sugerencia",
              "title": "title ok",
              "description": "ok desc",
              "appVersion": "0.2.0",
              "platform": "Android 36",
              "userEmail": "not-an-email"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/suggestions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
    }
}

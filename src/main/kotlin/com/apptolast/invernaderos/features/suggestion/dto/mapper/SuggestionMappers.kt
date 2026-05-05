package com.apptolast.invernaderos.features.suggestion.dto.mapper

import com.apptolast.invernaderos.features.suggestion.domain.model.Suggestion
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionCategory
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionDescription
import com.apptolast.invernaderos.features.suggestion.domain.model.SuggestionTitle
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionCommand
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionResult
import com.apptolast.invernaderos.features.suggestion.domain.port.output.SuggestionNotificationMeta
import com.apptolast.invernaderos.features.suggestion.dto.request.CreateSuggestionRequest
import com.apptolast.invernaderos.features.suggestion.dto.response.CreateSuggestionResponse
import java.time.Instant

private const val EMAIL_FAILED_WARNING =
    "La sugerencia se registró correctamente pero el equipo no recibió la notificación por email."

fun CreateSuggestionRequest.toCommand(submittedAt: Instant): SubmitSuggestionCommand =
    SubmitSuggestionCommand(
        suggestion = Suggestion(
            category = SuggestionCategory(category.trim()),
            title = SuggestionTitle(title.trim()),
            description = SuggestionDescription(description.trim())
        ),
        meta = SuggestionNotificationMeta(
            appVersion = appVersion.trim(),
            platform = platform.trim(),
            userEmail = userEmail.trim(),
            submittedAt = submittedAt
        )
    )

fun SubmitSuggestionResult.Success.toResponse(): CreateSuggestionResponse =
    CreateSuggestionResponse(
        issueNumber = issueRef.number,
        issueUrl = issueRef.htmlUrl,
        emailSent = true,
        warning = null
    )

fun SubmitSuggestionResult.PartialSuccess.toResponse(): CreateSuggestionResponse =
    CreateSuggestionResponse(
        issueNumber = issueRef.number,
        issueUrl = issueRef.htmlUrl,
        emailSent = false,
        warning = EMAIL_FAILED_WARNING
    )

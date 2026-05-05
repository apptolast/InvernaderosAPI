package com.apptolast.invernaderos.features.suggestion.infrastructure.adapter.input

import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionResult
import com.apptolast.invernaderos.features.suggestion.domain.port.input.SubmitSuggestionUseCase
import com.apptolast.invernaderos.features.suggestion.dto.mapper.toCommand
import com.apptolast.invernaderos.features.suggestion.dto.mapper.toResponse
import com.apptolast.invernaderos.features.suggestion.dto.request.CreateSuggestionRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/suggestions")
@Tag(name = "Suggestions", description = "User-submitted suggestions. Persisted as GitHub issues; team is notified by email.")
class SuggestionController(
    private val submitUseCase: SubmitSuggestionUseCase,
    private val clock: Clock
) {

    @PostMapping
    @Operation(summary = "Submit a new suggestion (creates a GitHub issue and notifies the team by email)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Suggestion accepted. emailSent=true means everything OK; emailSent=false means the issue was created but the email failed."),
            ApiResponse(responseCode = "400", description = "Validation error in the request body."),
            ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            ApiResponse(responseCode = "502", description = "Issue tracker (GitHub) is currently unavailable. The suggestion was NOT recorded; the user should retry.")
        ]
    )
    fun submit(@Valid @RequestBody request: CreateSuggestionRequest): ResponseEntity<Any> {
        val command = request.toCommand(submittedAt = Instant.now(clock))
        return when (val result = submitUseCase.execute(command)) {
            is SubmitSuggestionResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(result.toResponse())
            is SubmitSuggestionResult.PartialSuccess ->
                ResponseEntity.status(HttpStatus.CREATED).body(result.toResponse())
            is SubmitSuggestionResult.Failure ->
                ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(toIssueTrackerProblemDetail(result))
        }
    }

    private fun toIssueTrackerProblemDetail(failure: SubmitSuggestionResult.Failure): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY,
            "No se pudo registrar la sugerencia porque el sistema de seguimiento de incidencias no está disponible. Por favor, inténtalo de nuevo en unos minutos."
        )
        problem.title = "Issue Tracker Unavailable"
        problem.setProperty("timestamp", Instant.now(clock))
        problem.setProperty("errorCode", "ISSUE_TRACKER_UNAVAILABLE")
        problem.setProperty("errorId", UUID.randomUUID().toString())
        problem.setProperty("cause", failure.error.message)
        return problem
    }
}

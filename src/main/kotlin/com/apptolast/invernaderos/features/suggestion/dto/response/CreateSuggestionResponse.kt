package com.apptolast.invernaderos.features.suggestion.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response after submitting a suggestion. Returns the created GitHub issue reference and whether the team email was delivered.")
data class CreateSuggestionResponse(
    @Schema(description = "Number of the GitHub issue created", example = "42")
    val issueNumber: Int,

    @Schema(description = "Public URL of the GitHub issue", example = "https://github.com/apptolast/GreenhouseFronts/issues/42")
    val issueUrl: String,

    @Schema(description = "True if the notification email was delivered to the team. False means the issue was still recorded but the email failed (see warning).")
    val emailSent: Boolean,

    @Schema(description = "Human-readable warning when emailSent=false. Null otherwise.", nullable = true)
    val warning: String?
)

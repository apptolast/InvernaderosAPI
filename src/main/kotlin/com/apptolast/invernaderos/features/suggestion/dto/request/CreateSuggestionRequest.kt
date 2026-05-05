package com.apptolast.invernaderos.features.suggestion.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Request to submit a user suggestion. The backend will create a GitHub issue and notify the team by email.")
data class CreateSuggestionRequest(
    @field:NotBlank(message = "category is required")
    @field:Size(min = 1, max = 50, message = "category must be 1..50 characters")
    @Schema(description = "Free-form category chosen by the user", example = "Sugerencia")
    val category: String,

    @field:NotBlank(message = "title is required")
    @field:Size(min = 3, max = 200, message = "title must be 3..200 characters")
    @Schema(description = "Short title of the suggestion", example = "Mejorar el filtro de alertas")
    val title: String,

    @field:NotBlank(message = "description is required")
    @field:Size(min = 1, max = 5000, message = "description must be 1..5000 characters")
    @Schema(description = "Detailed description", example = "Sería útil poder filtrar por severidad y por sector a la vez")
    val description: String,

    @field:NotBlank(message = "appVersion is required")
    @field:Size(max = 50, message = "appVersion must be at most 50 characters")
    @Schema(description = "App version sent by the frontend", example = "0.2.0")
    val appVersion: String,

    @field:NotBlank(message = "platform is required")
    @field:Size(max = 100, message = "platform must be at most 100 characters")
    @Schema(description = "Device platform sent by the frontend", example = "Android 36")
    val platform: String,

    @field:NotBlank(message = "userEmail is required")
    @field:Email(message = "userEmail must be a valid email")
    @Schema(description = "Email of the user submitting the suggestion (added to email CC)", example = "user@example.com")
    val userEmail: String
)

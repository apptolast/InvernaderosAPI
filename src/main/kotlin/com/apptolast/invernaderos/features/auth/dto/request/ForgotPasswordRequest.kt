package com.apptolast.invernaderos.features.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for forgot password")
data class ForgotPasswordRequest(
        @field:NotBlank(message = "Email is required")
        @field:Email(message = "Invalid email format")
        @Schema(description = "User email address", example = "user@example.com")
        val email: String
)

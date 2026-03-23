package com.apptolast.invernaderos.features.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for user login")
data class LoginRequest(
        @field:NotBlank(message = "Username/Email is required")
        @Schema(description = "Username or email", example = "admin@company.com")
        val username: String,
        @field:NotBlank(message = "Password is required")
        @Schema(description = "User password", example = "password123")
        val password: String
)

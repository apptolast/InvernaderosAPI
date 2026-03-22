package com.apptolast.invernaderos.features.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Request object for reset password")
data class ResetPasswordRequest(
        @field:NotBlank(message = "Token is required")
        @Schema(description = "Reset token received via email")
        val token: String,
        @field:NotBlank(message = "New password is required")
        @field:Size(min = 6, message = "Password must be at least 6 characters long")
        @Schema(description = "New password", example = "newSecurePass123")
        val newPassword: String
)

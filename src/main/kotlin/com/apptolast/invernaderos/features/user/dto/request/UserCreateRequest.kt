package com.apptolast.invernaderos.features.user.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Request to create a new User")
data class UserCreateRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    @Schema(description = "Username for login", example = "jdoe")
    val username: String,

    @field:NotBlank
    @field:Email
    @Schema(description = "Email address", example = "jdoe@example.com")
    val email: String,

    @field:NotBlank
    @field:Size(min = 8)
    @Schema(description = "Raw password", example = "password123")
    val passwordRaw: String,

    @field:NotBlank
    @Schema(description = "User role (ADMIN, OPERATOR, VIEWER)", example = "OPERATOR")
    val role: String,

    @Schema(description = "Whether the user is active", example = "true")
    val isActive: Boolean = true
)

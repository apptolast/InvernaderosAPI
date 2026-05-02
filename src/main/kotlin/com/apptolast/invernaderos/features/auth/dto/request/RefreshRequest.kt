package com.apptolast.invernaderos.features.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request body for /api/v1/auth/refresh")
data class RefreshRequest(
    @field:NotBlank(message = "refreshToken is required")
    @Schema(description = "The refresh token previously issued by /login or /refresh", required = true)
    val refreshToken: String
)

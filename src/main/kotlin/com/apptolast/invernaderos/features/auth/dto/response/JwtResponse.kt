package com.apptolast.invernaderos.features.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object containing JWT and refresh tokens")
data class JwtResponse(
    @Schema(description = "JWT Access Token") val token: String,
    @Schema(description = "Token type", example = "Bearer") val type: String = "Bearer",
    @Schema(description = "Username/Email of the authenticated user") val username: String,
    @Schema(description = "List of roles assigned to the user") val roles: List<String>,
    @Schema(description = "Opaque refresh token (rotates on /refresh)") val refreshToken: String? = null,
    @Schema(description = "Access token TTL in seconds") val expiresIn: Long? = null,
    @Schema(description = "Refresh token TTL in seconds") val refreshExpiresIn: Long? = null
)

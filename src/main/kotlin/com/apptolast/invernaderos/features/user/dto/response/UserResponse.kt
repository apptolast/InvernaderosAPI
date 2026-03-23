package com.apptolast.invernaderos.features.user.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Response object representing a User")
data class UserResponse(
    @Schema(description = "Unique identifier of the user") val id: Long,
    @Schema(description = "Unique readable code of the user", example = "USR-00001") val code: String,
    @Schema(description = "ID of the tenant the user belongs to") val tenantId: Long,
    @Schema(description = "Username for login") val username: String,
    @Schema(description = "User's email address") val email: String,
    @Schema(description = "User's role") val role: String,
    @Schema(description = "Whether the user is active") val isActive: Boolean,
    @Schema(description = "Last login timestamp") val lastLogin: Instant?,
    @Schema(description = "Creation timestamp") val createdAt: Instant,
    @Schema(description = "Last update timestamp") val updatedAt: Instant
)

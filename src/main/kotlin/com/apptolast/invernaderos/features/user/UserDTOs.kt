package com.apptolast.invernaderos.features.user

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Response object representing a User")
data class UserResponse(
        @Schema(description = "Unique identifier of the user") val id: UUID,
        @Schema(description = "User's email address") val email: String,
        @Schema(description = "User's role") val role: String,
        @Schema(description = "ID of the tenant the user belongs to") val tenantId: UUID,
        @Schema(description = "Whether the user is active") val isActive: Boolean
)

fun User.toResponse() =
        UserResponse(
                id = this.id
                                ?: throw IllegalStateException(
                                        "User ID cannot be null when mapping to response"
                                ),
                email = this.email,
                role = this.role,
                tenantId = this.tenantId,
                isActive = this.isActive
        )

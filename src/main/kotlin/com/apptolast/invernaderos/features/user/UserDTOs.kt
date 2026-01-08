package com.apptolast.invernaderos.features.user

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Response object representing a User")
data class UserResponse(
    @Schema(description = "Unique identifier of the user") val id: Long,
    @Schema(description = "Username for login") val username: String,
    @Schema(description = "User's email address") val email: String,
    @Schema(description = "User's role") val role: String,
    @Schema(description = "ID of the tenant the user belongs to") val tenantId: Long,
    @Schema(description = "Whether the user is active") val isActive: Boolean,
    @Schema(description = "Last login timestamp") val lastLogin: Instant?,
    @Schema(description = "Creation timestamp") val createdAt: Instant,
    @Schema(description = "Last update timestamp") val updatedAt: Instant
)

@Schema(description = "Request to create a new User")
data class UserCreateRequest(
    @Schema(description = "Username for login", example = "jdoe")
    val username: String,

    @Schema(description = "Email address", example = "jdoe@example.com")
    val email: String,

    @Schema(description = "Raw password", example = "password123")
    val passwordRaw: String,

    @Schema(description = "User role (ADMIN, OPERATOR, VIEWER)", example = "OPERATOR")
    val role: String,

    @Schema(description = "Whether the user is active", example = "true")
    val isActive: Boolean? = true
)

@Schema(description = "Request to update an existing User")
data class UserUpdateRequest(
    @Schema(description = "Username for login")
    val username: String? = null,

    @Schema(description = "Email address")
    val email: String? = null,

    @Schema(description = "Raw password (if changing)")
    val passwordRaw: String? = null,

    @Schema(description = "User role (ADMIN, OPERATOR, VIEWER)")
    val role: String? = null,

    @Schema(description = "Whether the user is active")
    val isActive: Boolean? = null
)

fun User.toResponse() =
    UserResponse(
        id = this.id
            ?: throw IllegalStateException(
                "User ID cannot be null when mapping to response"
            ),
        username = this.username,
        email = this.email,
        role = this.role,
        tenantId = this.tenantId,
        isActive = this.isActive,
        lastLogin = this.lastLogin,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

package com.apptolast.invernaderos.features.user.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

@Schema(description = "Request to update an existing User")
data class UserUpdateRequest(
    @field:Size(min = 3, max = 50)
    @Schema(description = "Username for login")
    val username: String? = null,

    @field:Email
    @Schema(description = "Email address")
    val email: String? = null,

    @field:Size(min = 8)
    @Schema(description = "Raw password (if changing)")
    val passwordRaw: String? = null,

    @Schema(description = "User role (ADMIN, OPERATOR, VIEWER)")
    val role: String? = null,

    @Schema(description = "Whether the user is active")
    val isActive: Boolean? = null
)

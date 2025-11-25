package com.apptolast.invernaderos.features.user

import java.time.Instant
import java.util.UUID

data class UserDTO (
    val tenant_id: UUID,
    val username: String,
    val email: String,
    val password_hash: String,
    val role: String,
    val is_active: Boolean,
    val last_login: Instant,
    val created_at: Instant,
    val updated_at: Instant
)

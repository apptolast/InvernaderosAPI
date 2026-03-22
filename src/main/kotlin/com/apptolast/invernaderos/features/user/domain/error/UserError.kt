package com.apptolast.invernaderos.features.user.domain.error

import com.apptolast.invernaderos.features.shared.domain.model.TenantId

sealed interface UserError {
    val message: String

    data class NotFound(val id: Long, val tenantId: TenantId) : UserError {
        override val message: String
            get() = "User $id not found in tenant ${tenantId.value}"
    }

    data class DuplicateUsername(val username: String) : UserError {
        override val message: String
            get() = "User with username '$username' already exists"
    }

    data class DuplicateEmail(val email: String) : UserError {
        override val message: String
            get() = "User with email '$email' already exists"
    }

    data class InvalidRole(val role: String) : UserError {
        override val message: String
            get() = "Invalid role '$role'. Allowed roles: ADMIN, OPERATOR, VIEWER"
    }
}

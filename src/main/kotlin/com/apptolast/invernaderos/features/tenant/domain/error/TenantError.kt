package com.apptolast.invernaderos.features.tenant.domain.error

import com.apptolast.invernaderos.features.shared.domain.model.TenantId

sealed interface TenantError {
    val message: String

    data class NotFound(val id: TenantId) : TenantError {
        override val message: String
            get() = "Tenant ${id.value} not found"
    }

    data class DuplicateName(val name: String) : TenantError {
        override val message: String
            get() = "Tenant with name '$name' already exists"
    }

    data class DuplicateEmail(val email: String) : TenantError {
        override val message: String
            get() = "Tenant with email '$email' already exists"
    }
}

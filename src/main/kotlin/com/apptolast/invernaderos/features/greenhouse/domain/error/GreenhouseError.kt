package com.apptolast.invernaderos.features.greenhouse.domain.error

import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

sealed interface GreenhouseError {
    val message: String

    data class NotFound(val id: GreenhouseId, val tenantId: TenantId) : GreenhouseError {
        override val message: String
            get() = "Greenhouse ${id.value} not found for tenant ${tenantId.value}"
    }

    data class DuplicateName(val name: String, val tenantId: TenantId) : GreenhouseError {
        override val message: String
            get() = "Greenhouse '$name' already exists for tenant ${tenantId.value}"
    }
}

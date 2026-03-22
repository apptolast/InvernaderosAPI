package com.apptolast.invernaderos.features.tenant.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.Location
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus

interface CreateTenantUseCase {
    fun execute(command: CreateTenantCommand): Either<TenantError, Tenant>
}

data class CreateTenantCommand(
    val name: String,
    val email: String,
    val phone: String? = null,
    val province: String? = null,
    val country: String? = "España",
    val location: Location? = null,
    val status: TenantStatus = TenantStatus.ACTIVE
)

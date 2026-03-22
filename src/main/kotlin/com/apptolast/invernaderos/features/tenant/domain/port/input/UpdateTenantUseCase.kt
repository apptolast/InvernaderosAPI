package com.apptolast.invernaderos.features.tenant.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.Location
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus

interface UpdateTenantUseCase {
    fun execute(command: UpdateTenantCommand): Either<TenantError, Tenant>
}

data class UpdateTenantCommand(
    val id: TenantId,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val province: String? = null,
    val country: String? = null,
    val location: Location? = null,
    val status: TenantStatus? = null
)

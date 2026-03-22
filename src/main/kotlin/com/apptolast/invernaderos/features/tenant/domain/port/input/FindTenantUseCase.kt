package com.apptolast.invernaderos.features.tenant.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus

interface FindTenantUseCase {
    fun findById(id: TenantId): Either<TenantError, Tenant>
    fun findAll(filter: TenantFilter): List<Tenant>
}

data class TenantFilter(
    val search: String? = null,
    val province: String? = null,
    val status: TenantStatus? = null
)

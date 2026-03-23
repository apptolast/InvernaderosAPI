package com.apptolast.invernaderos.features.tenant.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.port.input.FindTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.input.TenantFilter
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort

class FindTenantUseCaseImpl(
    private val repository: TenantRepositoryPort
) : FindTenantUseCase {

    override fun findById(id: TenantId): Either<TenantError, Tenant> {
        val tenant = repository.findById(id)
            ?: return Either.Left(TenantError.NotFound(id))
        return Either.Right(tenant)
    }

    override fun findAll(filter: TenantFilter): List<Tenant> {
        return repository.findAll(filter)
    }
}

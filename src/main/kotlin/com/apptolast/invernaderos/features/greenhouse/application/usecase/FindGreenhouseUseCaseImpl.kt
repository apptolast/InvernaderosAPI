package com.apptolast.invernaderos.features.greenhouse.application.usecase

import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.FindGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class FindGreenhouseUseCaseImpl(
    private val repository: GreenhouseRepositoryPort
) : FindGreenhouseUseCase {

    override fun findById(id: GreenhouseId, tenantId: TenantId): Either<GreenhouseError, Greenhouse> {
        val greenhouse = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(GreenhouseError.NotFound(id, tenantId))
        return Either.Right(greenhouse)
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Greenhouse> {
        return repository.findAllByTenantId(tenantId)
    }
}

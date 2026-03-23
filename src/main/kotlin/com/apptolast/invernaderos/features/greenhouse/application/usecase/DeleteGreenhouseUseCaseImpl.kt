package com.apptolast.invernaderos.features.greenhouse.application.usecase

import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.DeleteGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class DeleteGreenhouseUseCaseImpl(
    private val repository: GreenhouseRepositoryPort
) : DeleteGreenhouseUseCase {

    override fun execute(id: GreenhouseId, tenantId: TenantId): Either<GreenhouseError, Unit> {
        if (repository.findByIdAndTenantId(id, tenantId) == null) {
            return Either.Left(GreenhouseError.NotFound(id, tenantId))
        }
        repository.delete(id, tenantId)
        return Either.Right(Unit)
    }
}

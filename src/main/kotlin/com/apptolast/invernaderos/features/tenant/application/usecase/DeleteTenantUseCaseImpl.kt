package com.apptolast.invernaderos.features.tenant.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.port.input.DeleteTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort

class DeleteTenantUseCaseImpl(
    private val repository: TenantRepositoryPort
) : DeleteTenantUseCase {

    override fun execute(id: TenantId): Either<TenantError, Unit> {
        if (repository.findById(id) == null) {
            return Either.Left(TenantError.NotFound(id))
        }
        repository.delete(id)
        return Either.Right(Unit)
    }
}

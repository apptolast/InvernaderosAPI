package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.port.input.DeleteAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class DeleteAlertUseCaseImpl(
    private val repository: AlertRepositoryPort
) : DeleteAlertUseCase {

    override fun execute(id: Long, tenantId: TenantId): Either<AlertError, Unit> {
        if (repository.findByIdAndTenantId(id, tenantId) == null) {
            return Either.Left(AlertError.NotFound(id, tenantId))
        }
        repository.delete(id, tenantId)
        return Either.Right(Unit)
    }
}

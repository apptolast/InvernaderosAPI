package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class FindAlertUseCaseImpl(
    private val repository: AlertRepositoryPort
) : FindAlertUseCase {

    override fun findByIdAndTenantId(id: Long, tenantId: TenantId): Either<AlertError, Alert> {
        val alert = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(AlertError.NotFound(id, tenantId))
        return Either.Right(alert)
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Alert> {
        return repository.findAllByTenantId(tenantId)
    }
}

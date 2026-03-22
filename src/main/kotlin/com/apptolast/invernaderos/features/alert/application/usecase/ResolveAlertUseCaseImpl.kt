package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.ResolveAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

class ResolveAlertUseCaseImpl(
    private val repository: AlertRepositoryPort
) : ResolveAlertUseCase {

    override fun resolve(id: Long, tenantId: TenantId, resolvedByUserId: Long?): Either<AlertError, Alert> {
        val existing = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(AlertError.NotFound(id, tenantId))

        if (existing.isResolved) {
            return Either.Left(AlertError.AlreadyResolved(id))
        }

        val resolved = existing.copy(
            isResolved = true,
            resolvedAt = Instant.now(),
            resolvedByUserId = resolvedByUserId,
            updatedAt = Instant.now()
        )

        return Either.Right(repository.save(resolved))
    }

    override fun reopen(id: Long, tenantId: TenantId): Either<AlertError, Alert> {
        val existing = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(AlertError.NotFound(id, tenantId))

        val reopened = existing.copy(
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            updatedAt = Instant.now()
        )

        return Either.Right(repository.save(reopened))
    }
}

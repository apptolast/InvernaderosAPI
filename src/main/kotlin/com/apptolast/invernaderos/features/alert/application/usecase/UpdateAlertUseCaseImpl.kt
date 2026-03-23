package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.UpdateAlertCommand
import com.apptolast.invernaderos.features.alert.domain.port.input.UpdateAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSectorValidationPort
import com.apptolast.invernaderos.features.shared.domain.Either
import java.time.Instant

class UpdateAlertUseCaseImpl(
    private val repository: AlertRepositoryPort,
    private val sectorValidation: AlertSectorValidationPort
) : UpdateAlertUseCase {

    override fun execute(command: UpdateAlertCommand): Either<AlertError, Alert> {
        val existing = repository.findByIdAndTenantId(command.id, command.tenantId)
            ?: return Either.Left(AlertError.NotFound(command.id, command.tenantId))

        val newSectorId = command.sectorId
        if (newSectorId != null && newSectorId != existing.sectorId) {
            if (!sectorValidation.existsByIdAndTenantId(newSectorId, command.tenantId)) {
                return Either.Left(AlertError.SectorNotOwnedByTenant(newSectorId, command.tenantId))
            }
        }

        val updated = existing.copy(
            sectorId = command.sectorId ?: existing.sectorId,
            alertTypeId = command.alertTypeId ?: existing.alertTypeId,
            severityId = command.severityId ?: existing.severityId,
            message = command.message ?: existing.message,
            description = command.description ?: existing.description,
            clientName = command.clientName ?: existing.clientName,
            updatedAt = Instant.now()
        )

        return Either.Right(repository.save(updated))
    }
}

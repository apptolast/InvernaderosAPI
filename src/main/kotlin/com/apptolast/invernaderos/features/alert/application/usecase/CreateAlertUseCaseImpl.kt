package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.CreateAlertCommand
import com.apptolast.invernaderos.features.alert.domain.port.input.CreateAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertCodeGenerator
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSectorValidationPort
import com.apptolast.invernaderos.features.shared.domain.Either
import java.time.Instant

class CreateAlertUseCaseImpl(
    private val repository: AlertRepositoryPort,
    private val codeGenerator: AlertCodeGenerator,
    private val sectorValidation: AlertSectorValidationPort
) : CreateAlertUseCase {

    override fun execute(command: CreateAlertCommand): Either<AlertError, Alert> {
        if (!sectorValidation.existsByIdAndTenantId(command.sectorId, command.tenantId)) {
            return Either.Left(AlertError.SectorNotOwnedByTenant(command.sectorId, command.tenantId))
        }

        val now = Instant.now()
        val alert = Alert(
            id = null,
            code = codeGenerator.generate(),
            tenantId = command.tenantId,
            sectorId = command.sectorId,
            sectorCode = null,
            alertTypeId = command.alertTypeId,
            alertTypeName = null,
            severityId = command.severityId,
            severityName = null,
            severityLevel = null,
            message = command.message,
            description = command.description,
            clientName = command.clientName,
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            resolvedByUserName = null,
            createdAt = now,
            updatedAt = now
        )

        return Either.Right(repository.save(alert))
    }
}

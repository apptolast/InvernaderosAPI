package com.apptolast.invernaderos.features.greenhouse.application.usecase

import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.UpdateGreenhouseCommand
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.UpdateGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import java.time.Instant

class UpdateGreenhouseUseCaseImpl(
    private val repository: GreenhouseRepositoryPort
) : UpdateGreenhouseUseCase {

    override fun execute(command: UpdateGreenhouseCommand): Either<GreenhouseError, Greenhouse> {
        val existing = repository.findByIdAndTenantId(command.id, command.tenantId)
            ?: return Either.Left(GreenhouseError.NotFound(command.id, command.tenantId))

        // Validate name uniqueness if name is being changed
        command.name?.let { newName ->
            if (newName != existing.name && repository.existsByNameAndTenantId(newName, command.tenantId)) {
                return Either.Left(GreenhouseError.DuplicateName(newName, command.tenantId))
            }
        }

        val updated = existing.copy(
            name = command.name ?: existing.name,
            location = command.location ?: existing.location,
            areaM2 = command.areaM2 ?: existing.areaM2,
            timezone = command.timezone ?: existing.timezone,
            isActive = command.isActive ?: existing.isActive,
            updatedAt = Instant.now()
        )

        return Either.Right(repository.save(updated))
    }
}

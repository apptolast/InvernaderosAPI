package com.apptolast.invernaderos.features.greenhouse.application.usecase

import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.CreateGreenhouseCommand
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.CreateGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseCodeGenerator
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import java.time.Instant

class CreateGreenhouseUseCaseImpl(
    private val repository: GreenhouseRepositoryPort,
    private val codeGenerator: GreenhouseCodeGenerator
) : CreateGreenhouseUseCase {

    override fun execute(command: CreateGreenhouseCommand): Either<GreenhouseError, Greenhouse> {
        if (repository.existsByNameAndTenantId(command.name, command.tenantId)) {
            return Either.Left(GreenhouseError.DuplicateName(command.name, command.tenantId))
        }

        val now = Instant.now()
        val greenhouse = Greenhouse(
            id = null,
            code = codeGenerator.generate(),
            tenantId = command.tenantId,
            name = command.name,
            location = command.location,
            areaM2 = command.areaM2,
            timezone = command.timezone,
            isActive = command.isActive,
            createdAt = now,
            updatedAt = now
        )

        return Either.Right(repository.save(greenhouse))
    }
}

package com.apptolast.invernaderos.features.tenant.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.port.input.CreateTenantCommand
import com.apptolast.invernaderos.features.tenant.domain.port.input.CreateTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantCodeGenerator
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort
import java.time.Instant

class CreateTenantUseCaseImpl(
    private val repository: TenantRepositoryPort,
    private val codeGenerator: TenantCodeGenerator
) : CreateTenantUseCase {

    override fun execute(command: CreateTenantCommand): Either<TenantError, Tenant> {
        if (repository.existsByName(command.name)) {
            return Either.Left(TenantError.DuplicateName(command.name))
        }

        if (repository.existsByEmail(command.email)) {
            return Either.Left(TenantError.DuplicateEmail(command.email))
        }

        val now = Instant.now()
        val tenant = Tenant(
            id = null,
            code = codeGenerator.generate(),
            name = command.name,
            email = command.email,
            phone = command.phone,
            province = command.province,
            country = command.country,
            location = command.location,
            status = command.status,
            createdAt = now,
            updatedAt = now
        )

        return Either.Right(repository.save(tenant))
    }
}

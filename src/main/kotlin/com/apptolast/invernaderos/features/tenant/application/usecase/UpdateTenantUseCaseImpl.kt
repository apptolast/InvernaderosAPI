package com.apptolast.invernaderos.features.tenant.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.port.input.UpdateTenantCommand
import com.apptolast.invernaderos.features.tenant.domain.port.input.UpdateTenantUseCase
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort
import java.time.Instant

class UpdateTenantUseCaseImpl(
    private val repository: TenantRepositoryPort
) : UpdateTenantUseCase {

    override fun execute(command: UpdateTenantCommand): Either<TenantError, Tenant> {
        val existing = repository.findById(command.id)
            ?: return Either.Left(TenantError.NotFound(command.id))

        command.name?.let { newName ->
            if (newName != existing.name && repository.existsByName(newName)) {
                return Either.Left(TenantError.DuplicateName(newName))
            }
        }

        command.email?.let { newEmail ->
            if (newEmail != existing.email && repository.existsByEmail(newEmail)) {
                return Either.Left(TenantError.DuplicateEmail(newEmail))
            }
        }

        val updated = existing.copy(
            name = command.name ?: existing.name,
            email = command.email ?: existing.email,
            phone = command.phone ?: existing.phone,
            province = command.province ?: existing.province,
            country = command.country ?: existing.country,
            location = command.location ?: existing.location,
            status = command.status ?: existing.status,
            updatedAt = Instant.now()
        )

        return Either.Right(repository.save(updated))
    }
}

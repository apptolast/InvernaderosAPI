package com.apptolast.invernaderos.features.user.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.port.input.DeleteUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort

class DeleteUserUseCaseImpl(
    private val repository: UserRepositoryPort
) : DeleteUserUseCase {

    override fun execute(id: Long, tenantId: TenantId): Either<UserError, Unit> {
        val exists = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(UserError.NotFound(id, tenantId))

        repository.delete(exists.id!!, tenantId)
        return Either.Right(Unit)
    }
}

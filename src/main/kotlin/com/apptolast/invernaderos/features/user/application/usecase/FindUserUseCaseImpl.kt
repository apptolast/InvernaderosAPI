package com.apptolast.invernaderos.features.user.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.port.input.FindUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort

class FindUserUseCaseImpl(
    private val repository: UserRepositoryPort
) : FindUserUseCase {

    override fun findByIdAndTenantId(id: Long, tenantId: TenantId): Either<UserError, User> {
        val user = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(UserError.NotFound(id, tenantId))
        return Either.Right(user)
    }

    override fun findAllByTenantId(tenantId: TenantId): List<User> {
        return repository.findAllByTenantId(tenantId)
    }
}

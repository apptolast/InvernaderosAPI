package com.apptolast.invernaderos.features.user.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User

interface CreateUserUseCase {
    fun execute(command: CreateUserCommand): Either<UserError, User>
}

data class CreateUserCommand(
    val tenantId: TenantId,
    val username: String,
    val email: String,
    val passwordRaw: String,
    val role: String,
    val isActive: Boolean = true
)

package com.apptolast.invernaderos.features.user.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User

interface UpdateUserUseCase {
    fun execute(command: UpdateUserCommand): Either<UserError, User>
}

data class UpdateUserCommand(
    val id: Long,
    val tenantId: TenantId,
    val username: String? = null,
    val email: String? = null,
    val passwordRaw: String? = null,
    val role: String? = null,
    val isActive: Boolean? = null
)

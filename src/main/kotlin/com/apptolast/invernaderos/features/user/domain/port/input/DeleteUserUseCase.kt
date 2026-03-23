package com.apptolast.invernaderos.features.user.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.error.UserError

interface DeleteUserUseCase {
    fun execute(id: Long, tenantId: TenantId): Either<UserError, Unit>
}

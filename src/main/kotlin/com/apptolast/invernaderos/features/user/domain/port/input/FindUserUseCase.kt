package com.apptolast.invernaderos.features.user.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.model.User

interface FindUserUseCase {
    fun findByIdAndTenantId(id: Long, tenantId: TenantId): Either<UserError, User>
    fun findAllByTenantId(tenantId: TenantId): List<User>
}

package com.apptolast.invernaderos.features.tenant.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.domain.error.TenantError

interface DeleteTenantUseCase {
    fun execute(id: TenantId): Either<TenantError, Unit>
}

package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface DeleteAlertUseCase {
    fun execute(id: Long, tenantId: TenantId): Either<AlertError, Unit>
}

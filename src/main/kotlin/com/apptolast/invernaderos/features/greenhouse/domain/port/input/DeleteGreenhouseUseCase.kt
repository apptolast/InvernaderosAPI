package com.apptolast.invernaderos.features.greenhouse.domain.port.input

import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface DeleteGreenhouseUseCase {
    fun execute(id: GreenhouseId, tenantId: TenantId): Either<GreenhouseError, Unit>
}

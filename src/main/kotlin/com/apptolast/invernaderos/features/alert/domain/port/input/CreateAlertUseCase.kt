package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface CreateAlertUseCase {
    fun execute(command: CreateAlertCommand): Either<AlertError, Alert>
}

data class CreateAlertCommand(
    val tenantId: TenantId,
    val sectorId: SectorId,
    val alertTypeId: Short? = null,
    val severityId: Short? = null,
    val message: String? = null,
    val description: String? = null
)

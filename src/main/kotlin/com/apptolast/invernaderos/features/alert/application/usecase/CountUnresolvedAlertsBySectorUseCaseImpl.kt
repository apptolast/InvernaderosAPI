package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.port.input.CountUnresolvedAlertsBySectorUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class CountUnresolvedAlertsBySectorUseCaseImpl(
    private val repository: AlertRepositoryPort,
) : CountUnresolvedAlertsBySectorUseCase {

    override fun execute(sectorId: Long, tenantId: TenantId): Long {
        require(sectorId > 0) { "sectorId must be positive" }
        return repository.countUnresolvedBySectorAndTenant(sectorId, tenantId)
    }
}

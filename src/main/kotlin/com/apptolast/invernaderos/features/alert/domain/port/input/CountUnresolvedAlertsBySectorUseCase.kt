package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface CountUnresolvedAlertsBySectorUseCase {
    fun execute(sectorId: Long, tenantId: TenantId): Long
}

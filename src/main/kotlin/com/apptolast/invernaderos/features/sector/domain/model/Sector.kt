package com.apptolast.invernaderos.features.sector.domain.model

import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

data class Sector(
    val id: SectorId?,
    val code: String,
    val tenantId: TenantId,
    val greenhouseId: GreenhouseId,
    val greenhouseCode: String?,
    val name: String?
)

package com.apptolast.invernaderos.features.alert.domain.model.query

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

enum class MttrGroupBy { SEVERITY, TYPE, SECTOR, CODE }

data class MttrStatsQuery(
    val tenantId: TenantId,
    val from: Instant,
    val to: Instant,
    val groupBy: MttrGroupBy,
)

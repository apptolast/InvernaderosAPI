package com.apptolast.invernaderos.features.alert.domain.model.query

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

enum class ActiveDurationGroupBy { CODE, SECTOR }

data class ActiveDurationStatsQuery(
    val tenantId: TenantId,
    val from: Instant,
    val to: Instant,
    val groupBy: ActiveDurationGroupBy,
)

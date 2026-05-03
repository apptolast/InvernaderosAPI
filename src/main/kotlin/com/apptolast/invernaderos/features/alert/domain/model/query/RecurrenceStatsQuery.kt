package com.apptolast.invernaderos.features.alert.domain.model.query

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

enum class RecurrenceGroupBy { CODE, TYPE, SEVERITY, SECTOR, GREENHOUSE }

data class RecurrenceStatsQuery(
    val tenantId: TenantId,
    val from: Instant,
    val to: Instant,
    val groupBy: RecurrenceGroupBy,
    val limit: Int,
)

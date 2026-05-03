package com.apptolast.invernaderos.features.alert.domain.model.query

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

enum class TimeseriesBucket { HOUR, DAY, WEEK, MONTH }

enum class TimeseriesGroupBy { SEVERITY, TYPE }

data class TimeseriesStatsQuery(
    val tenantId: TenantId,
    val from: Instant,
    val to: Instant,
    val bucket: TimeseriesBucket,
    val groupBy: TimeseriesGroupBy,
)

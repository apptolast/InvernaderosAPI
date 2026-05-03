package com.apptolast.invernaderos.features.alert.domain.model.query

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

data class AlertEpisodesQuery(
    val tenantId: TenantId,
    val from: Instant,
    val to: Instant,
    val severityIds: List<Short>,
    val sectorIds: List<Long>,
    val codes: List<String>,
    val onlyClosed: Boolean,
    val page: Int,
    val size: Int,
)

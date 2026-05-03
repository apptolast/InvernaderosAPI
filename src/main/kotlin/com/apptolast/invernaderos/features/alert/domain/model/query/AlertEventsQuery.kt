package com.apptolast.invernaderos.features.alert.domain.model.query

import com.apptolast.invernaderos.features.alert.domain.model.TransitionKind
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

data class AlertEventsQuery(
    val tenantId: TenantId,
    val from: Instant,
    val to: Instant,
    val sources: List<String>,
    val severityIds: List<Short>,
    val alertTypeIds: List<Short>,
    val sectorIds: List<Long>,
    val greenhouseIds: List<Long>,
    val codes: List<String>,
    val actorUserIds: List<Long>,
    val transitionKind: TransitionKind,
    val page: Int,
    val size: Int,
)

package com.apptolast.invernaderos.features.alert.domain.model.query

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

enum class ActorStatsRole { RESOLVER, OPENER }

data class ByActorStatsQuery(
    val tenantId: TenantId,
    val from: Instant,
    val to: Instant,
    val role: ActorStatsRole,
)

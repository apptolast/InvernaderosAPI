package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.AlertStatsSummary
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

interface AlertSummaryStatsUseCase {
    fun execute(tenantId: TenantId, from: Instant, to: Instant): AlertStatsSummary
}

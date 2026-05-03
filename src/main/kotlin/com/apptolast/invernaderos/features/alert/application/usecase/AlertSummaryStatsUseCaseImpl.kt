package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.AlertStatsSummary
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertSummaryStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

class AlertSummaryStatsUseCaseImpl(
    private val statsPort: AlertStatsQueryPort,
) : AlertSummaryStatsUseCase {

    override fun execute(tenantId: TenantId, from: Instant, to: Instant): AlertStatsSummary {
        require(!from.isAfter(to)) { "from must not be after to" }
        return statsPort.summary(tenantId, from, to)
    }
}

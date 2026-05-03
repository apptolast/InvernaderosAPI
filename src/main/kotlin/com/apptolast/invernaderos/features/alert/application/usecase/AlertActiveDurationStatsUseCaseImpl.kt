package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.ActiveDurationBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationStatsQuery
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertActiveDurationStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort

class AlertActiveDurationStatsUseCaseImpl(
    private val statsPort: AlertStatsQueryPort,
) : AlertActiveDurationStatsUseCase {

    override fun execute(query: ActiveDurationStatsQuery): List<ActiveDurationBucket> {
        require(!query.from.isAfter(query.to)) { "from must not be after to" }
        return statsPort.activeDuration(query)
    }
}

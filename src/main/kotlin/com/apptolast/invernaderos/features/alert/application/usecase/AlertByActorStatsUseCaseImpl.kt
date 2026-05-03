package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.ByActorBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.ByActorStatsQuery
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertByActorStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort

class AlertByActorStatsUseCaseImpl(
    private val statsPort: AlertStatsQueryPort,
) : AlertByActorStatsUseCase {

    override fun execute(query: ByActorStatsQuery): List<ByActorBucket> {
        require(!query.from.isAfter(query.to)) { "from must not be after to" }
        return statsPort.byActor(query)
    }
}

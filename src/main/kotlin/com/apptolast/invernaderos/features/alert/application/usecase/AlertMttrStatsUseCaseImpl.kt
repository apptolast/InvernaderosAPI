package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.MttrBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrStatsQuery
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertMttrStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort

class AlertMttrStatsUseCaseImpl(
    private val statsPort: AlertStatsQueryPort,
) : AlertMttrStatsUseCase {

    override fun execute(query: MttrStatsQuery): List<MttrBucket> {
        require(!query.from.isAfter(query.to)) { "from must not be after to" }
        return statsPort.mttr(query)
    }
}

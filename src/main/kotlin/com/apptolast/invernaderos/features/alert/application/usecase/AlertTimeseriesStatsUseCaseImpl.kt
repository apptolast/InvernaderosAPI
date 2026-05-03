package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.TimeseriesDataPoint
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesStatsQuery
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertTimeseriesStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort

class AlertTimeseriesStatsUseCaseImpl(
    private val statsPort: AlertStatsQueryPort,
) : AlertTimeseriesStatsUseCase {

    override fun execute(query: TimeseriesStatsQuery): List<TimeseriesDataPoint> {
        require(!query.from.isAfter(query.to)) { "from must not be after to" }
        return statsPort.timeseries(query)
    }
}

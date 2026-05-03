package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.TimeseriesDataPoint
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesStatsQuery

interface AlertTimeseriesStatsUseCase {
    fun execute(query: TimeseriesStatsQuery): List<TimeseriesDataPoint>
}

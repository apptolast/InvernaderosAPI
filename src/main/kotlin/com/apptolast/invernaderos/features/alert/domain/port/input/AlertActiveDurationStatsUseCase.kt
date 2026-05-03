package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.ActiveDurationBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationStatsQuery

interface AlertActiveDurationStatsUseCase {
    fun execute(query: ActiveDurationStatsQuery): List<ActiveDurationBucket>
}

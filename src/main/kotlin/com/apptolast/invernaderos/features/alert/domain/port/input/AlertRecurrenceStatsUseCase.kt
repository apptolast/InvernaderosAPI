package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.RecurrenceBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceStatsQuery

interface AlertRecurrenceStatsUseCase {
    fun execute(query: RecurrenceStatsQuery): List<RecurrenceBucket>
}

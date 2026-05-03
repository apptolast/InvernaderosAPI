package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.RecurrenceBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceStatsQuery
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertRecurrenceStatsUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort

private const val MAX_LIMIT = 100

class AlertRecurrenceStatsUseCaseImpl(
    private val statsPort: AlertStatsQueryPort,
) : AlertRecurrenceStatsUseCase {

    override fun execute(query: RecurrenceStatsQuery): List<RecurrenceBucket> {
        require(!query.from.isAfter(query.to)) { "from must not be after to" }
        require(query.limit in 1..MAX_LIMIT) { "limit must be 1...$MAX_LIMIT" }
        return statsPort.recurrence(query)
    }
}

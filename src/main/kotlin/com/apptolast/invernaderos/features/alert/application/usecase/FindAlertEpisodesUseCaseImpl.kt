package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.AlertEpisode
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEpisodesQuery
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertEpisodesUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertHistoryQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.PagedResult

private const val MAX_PAGE_SIZE = 200

/**
 * Plain Kotlin use case for alert episode queries. Validates inputs and delegates
 * to [AlertHistoryQueryPort]. No Spring annotations — wiring is in AlertModuleConfig.
 */
class FindAlertEpisodesUseCaseImpl(
    private val historyQueryPort: AlertHistoryQueryPort,
) : FindAlertEpisodesUseCase {

    override fun findEpisodes(query: AlertEpisodesQuery): PagedResult<AlertEpisode> {
        require(!query.from.isAfter(query.to)) { "from must not be after to" }
        require(query.page >= 0) { "page must be non-negative" }
        require(query.size in 1..MAX_PAGE_SIZE) { "size must be 1...$MAX_PAGE_SIZE" }
        return historyQueryPort.findEpisodes(query)
    }
}

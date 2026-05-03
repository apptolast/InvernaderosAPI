package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.AlertEpisode
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEpisodesQuery
import com.apptolast.invernaderos.features.shared.domain.model.PagedResult

interface FindAlertEpisodesUseCase {
    fun findEpisodes(query: AlertEpisodesQuery): PagedResult<AlertEpisode>
}

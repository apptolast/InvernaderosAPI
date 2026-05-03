package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.ByActorBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.ByActorStatsQuery

interface AlertByActorStatsUseCase {
    fun execute(query: ByActorStatsQuery): List<ByActorBucket>
}

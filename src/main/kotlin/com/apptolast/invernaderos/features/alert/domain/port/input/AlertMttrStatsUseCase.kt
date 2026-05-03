package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.MttrBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrStatsQuery

interface AlertMttrStatsUseCase {
    fun execute(query: MttrStatsQuery): List<MttrBucket>
}

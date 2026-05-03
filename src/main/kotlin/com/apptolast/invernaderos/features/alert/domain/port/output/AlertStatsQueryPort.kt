package com.apptolast.invernaderos.features.alert.domain.port.output

import com.apptolast.invernaderos.features.alert.domain.model.ActiveDurationBucket
import com.apptolast.invernaderos.features.alert.domain.model.AlertStatsSummary
import com.apptolast.invernaderos.features.alert.domain.model.ByActorBucket
import com.apptolast.invernaderos.features.alert.domain.model.MttrBucket
import com.apptolast.invernaderos.features.alert.domain.model.RecurrenceBucket
import com.apptolast.invernaderos.features.alert.domain.model.TimeseriesDataPoint
import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.ByActorStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesStatsQuery
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

interface AlertStatsQueryPort {
    fun recurrence(query: RecurrenceStatsQuery): List<RecurrenceBucket>
    fun mttr(query: MttrStatsQuery): List<MttrBucket>
    fun timeseries(query: TimeseriesStatsQuery): List<TimeseriesDataPoint>
    fun activeDuration(query: ActiveDurationStatsQuery): List<ActiveDurationBucket>
    fun byActor(query: ByActorStatsQuery): List<ByActorBucket>
    fun summary(tenantId: TenantId, from: Instant, to: Instant): AlertStatsSummary
}

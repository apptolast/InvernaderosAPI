package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.ActorStatsRole
import com.apptolast.invernaderos.features.alert.domain.model.query.ByActorStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceStatsQuery
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesStatsQuery
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * SQL contract tests against the live DEV `metadata` Postgres for the
 * statistics adapter. Same rationale as AlertHistoryQueryAdapterIntegrationTest:
 * the goal is "SQL parses and runs against PG 16", not row content.
 *
 * Covers all 6 stat endpoints (recurrence / mttr / timeseries / activeDuration
 * / byActor / summary) across every enum branch of their groupBy / role / bucket
 * dimensions, so any future SQL drift (column rename, reserved-word alias,
 * dialect mismatch) breaks at this test layer rather than at runtime in prod.
 */
@SpringBootTest
class AlertStatsQueryAdapterIntegrationTest {

    @Autowired
    private lateinit var adapter: AlertStatsQueryAdapter

    private val anyTenant = TenantId(1L)
    private val from: Instant get() = Instant.now().minus(30, ChronoUnit.DAYS)
    private val to: Instant get() = Instant.now()

    @Test
    fun `recurrence parses and executes for every groupBy`() {
        for (gb in RecurrenceGroupBy.entries) {
            val r = adapter.recurrence(RecurrenceStatsQuery(anyTenant, from, to, gb, limit = 10))
            assertThat(r).isNotNull
        }
    }

    @Test
    fun `mttr parses and executes for every groupBy`() {
        for (gb in MttrGroupBy.entries) {
            val r = adapter.mttr(MttrStatsQuery(anyTenant, from, to, gb))
            assertThat(r).isNotNull
        }
    }

    @Test
    fun `timeseries parses and executes for every bucket and groupBy`() {
        for (b in TimeseriesBucket.entries) {
            for (gb in TimeseriesGroupBy.entries) {
                val r = adapter.timeseries(TimeseriesStatsQuery(anyTenant, from, to, b, gb))
                assertThat(r).isNotNull
            }
        }
    }

    @Test
    fun `activeDuration parses and executes for every groupBy`() {
        for (gb in ActiveDurationGroupBy.entries) {
            val r = adapter.activeDuration(ActiveDurationStatsQuery(anyTenant, from, to, gb))
            assertThat(r).isNotNull
        }
    }

    @Test
    fun `byActor parses and executes for every role`() {
        for (role in ActorStatsRole.entries) {
            val r = adapter.byActor(ByActorStatsQuery(anyTenant, from, to, role))
            assertThat(r).isNotNull
        }
    }

    @Test
    fun `summary parses and executes`() {
        val r = adapter.summary(anyTenant, from, to)
        assertThat(r).isNotNull
        // sanity: all numeric counts must be non-negative
        assertThat(r.totalActiveNow).isGreaterThanOrEqualTo(0L)
        assertThat(r.openedToday).isGreaterThanOrEqualTo(0L)
        assertThat(r.closedToday).isGreaterThanOrEqualTo(0L)
        assertThat(r.top3RecurrentCodesThisWeek).isNotNull
    }
}

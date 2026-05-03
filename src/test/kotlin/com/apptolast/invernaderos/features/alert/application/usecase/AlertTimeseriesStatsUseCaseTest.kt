package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.TimeseriesDataPoint
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.TimeseriesStatsQuery
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStatsQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class AlertTimeseriesStatsUseCaseTest {

    private val statsPort = mockk<AlertStatsQueryPort>()
    private val useCase = AlertTimeseriesStatsUseCaseImpl(statsPort)

    private val tenantId = TenantId(10L)
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val from = now.minusSeconds(86400 * 7)

    private fun buildQuery(
        from: Instant = this.from,
        to: Instant = this.now,
        bucket: TimeseriesBucket = TimeseriesBucket.DAY,
        groupBy: TimeseriesGroupBy = TimeseriesGroupBy.SEVERITY,
    ) = TimeseriesStatsQuery(
        tenantId = tenantId,
        from = from,
        to = to,
        bucket = bucket,
        groupBy = groupBy,
    )

    @Test
    fun `should return port result unchanged when query is valid`() {
        val query = buildQuery()
        val expected = listOf(
            TimeseriesDataPoint(
                bucketStart = now.minusSeconds(86400),
                key = "WARNING",
                opened = 3L,
                closed = 2L,
            )
        )
        every { statsPort.timeseries(query) } returns expected

        val result = useCase.execute(query)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should propagate query to the port including bucket granularity`() {
        val querySlot = slot<TimeseriesStatsQuery>()
        val query = buildQuery(bucket = TimeseriesBucket.HOUR, groupBy = TimeseriesGroupBy.TYPE)
        every { statsPort.timeseries(capture(querySlot)) } returns emptyList()

        useCase.execute(query)

        assertThat(querySlot.captured.bucket).isEqualTo(TimeseriesBucket.HOUR)
        assertThat(querySlot.captured.groupBy).isEqualTo(TimeseriesGroupBy.TYPE)
        assertThat(querySlot.captured.tenantId).isEqualTo(tenantId)
    }

    @Test
    fun `should throw when from is after to`() {
        val query = buildQuery(from = now, to = now.minusSeconds(1))

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("from must not be after to")
    }

    @Test
    fun `should return empty list when port returns empty`() {
        val query = buildQuery()
        every { statsPort.timeseries(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should not call port when from is after to`() {
        val query = buildQuery(from = now, to = now.minusSeconds(3600))

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { statsPort.timeseries(any()) }
    }

    @Test
    fun `should support WEEK bucket granularity`() {
        val query = buildQuery(bucket = TimeseriesBucket.WEEK)
        every { statsPort.timeseries(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isNotNull
    }

    @Test
    fun `should support MONTH bucket granularity`() {
        val query = buildQuery(bucket = TimeseriesBucket.MONTH)
        every { statsPort.timeseries(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isNotNull
    }
}

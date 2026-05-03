package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.MttrBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.MttrStatsQuery
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

class AlertMttrStatsUseCaseTest {

    private val statsPort = mockk<AlertStatsQueryPort>()
    private val useCase = AlertMttrStatsUseCaseImpl(statsPort)

    private val tenantId = TenantId(10L)
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val from = now.minusSeconds(86400)

    private fun buildQuery(
        from: Instant = this.from,
        to: Instant = this.now,
        groupBy: MttrGroupBy = MttrGroupBy.SEVERITY,
    ) = MttrStatsQuery(
        tenantId = tenantId,
        from = from,
        to = to,
        groupBy = groupBy,
    )

    @Test
    fun `should return port result unchanged when query is valid`() {
        val query = buildQuery()
        val expected = listOf(
            MttrBucket(
                key = "WARNING",
                label = "Warning",
                mttrSeconds = 1800.0,
                p50Seconds = 1200.0,
                p95Seconds = 3600.0,
                p99Seconds = 7200.0,
                sampleSize = 5L,
            )
        )
        every { statsPort.mttr(query) } returns expected

        val result = useCase.execute(query)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should propagate query to the port unchanged`() {
        val querySlot = slot<MttrStatsQuery>()
        val query = buildQuery(groupBy = MttrGroupBy.CODE)
        every { statsPort.mttr(capture(querySlot)) } returns emptyList()

        useCase.execute(query)

        assertThat(querySlot.captured.groupBy).isEqualTo(MttrGroupBy.CODE)
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
        every { statsPort.mttr(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should not call port when from is after to`() {
        val query = buildQuery(from = now, to = now.minusSeconds(3600))

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { statsPort.mttr(any()) }
    }

    @Test
    fun `should support all groupBy variants`() {
        MttrGroupBy.entries.forEach { groupBy ->
            val query = buildQuery(groupBy = groupBy)
            every { statsPort.mttr(query) } returns emptyList()

            val result = useCase.execute(query)

            assertThat(result).isNotNull
        }
    }

    @Test
    fun `should accept from equal to to (point-in-time boundary)`() {
        val query = buildQuery(from = now, to = now)
        every { statsPort.mttr(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isEmpty()
    }
}

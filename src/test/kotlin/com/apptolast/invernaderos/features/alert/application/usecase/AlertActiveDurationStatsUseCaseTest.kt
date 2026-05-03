package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.ActiveDurationBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.ActiveDurationStatsQuery
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

class AlertActiveDurationStatsUseCaseTest {

    private val statsPort = mockk<AlertStatsQueryPort>()
    private val useCase = AlertActiveDurationStatsUseCaseImpl(statsPort)

    private val tenantId = TenantId(10L)
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val from = now.minusSeconds(86400)

    private fun buildQuery(
        from: Instant = this.from,
        to: Instant = this.now,
        groupBy: ActiveDurationGroupBy = ActiveDurationGroupBy.CODE,
    ) = ActiveDurationStatsQuery(
        tenantId = tenantId,
        from = from,
        to = to,
        groupBy = groupBy,
    )

    @Test
    fun `should return port result unchanged when query is valid`() {
        val query = buildQuery()
        val expected = listOf(
            ActiveDurationBucket(key = "ALT-001", label = "ALT-001", totalActiveSeconds = 7200L)
        )
        every { statsPort.activeDuration(query) } returns expected

        val result = useCase.execute(query)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should propagate query to the port including groupBy`() {
        val querySlot = slot<ActiveDurationStatsQuery>()
        val query = buildQuery(groupBy = ActiveDurationGroupBy.SECTOR)
        every { statsPort.activeDuration(capture(querySlot)) } returns emptyList()

        useCase.execute(query)

        assertThat(querySlot.captured.groupBy).isEqualTo(ActiveDurationGroupBy.SECTOR)
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
        every { statsPort.activeDuration(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should not call port when from is after to`() {
        val query = buildQuery(from = now, to = now.minusSeconds(3600))

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { statsPort.activeDuration(any()) }
    }

    @Test
    fun `should support groupBy SECTOR`() {
        val query = buildQuery(groupBy = ActiveDurationGroupBy.SECTOR)
        every { statsPort.activeDuration(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isNotNull
    }

    @Test
    fun `should accept from equal to to`() {
        val query = buildQuery(from = now, to = now)
        every { statsPort.activeDuration(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isEmpty()
    }
}

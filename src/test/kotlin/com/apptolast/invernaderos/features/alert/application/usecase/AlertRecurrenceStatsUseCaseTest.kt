package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.RecurrenceBucket
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceGroupBy
import com.apptolast.invernaderos.features.alert.domain.model.query.RecurrenceStatsQuery
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

class AlertRecurrenceStatsUseCaseTest {

    private val statsPort = mockk<AlertStatsQueryPort>()
    private val useCase = AlertRecurrenceStatsUseCaseImpl(statsPort)

    private val tenantId = TenantId(10L)
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val from = now.minusSeconds(86400)

    private fun buildQuery(
        from: Instant = this.from,
        to: Instant = this.now,
        groupBy: RecurrenceGroupBy = RecurrenceGroupBy.CODE,
        limit: Int = 10,
    ) = RecurrenceStatsQuery(
        tenantId = tenantId,
        from = from,
        to = to,
        groupBy = groupBy,
        limit = limit,
    )

    @Test
    fun `should return port result unchanged when query is valid`() {
        val query = buildQuery()
        val expected = listOf(
            RecurrenceBucket(key = "ALT-001", label = "ALT-001", count = 17L, lastSeenAt = now)
        )
        every { statsPort.recurrence(query) } returns expected

        val result = useCase.execute(query)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should propagate query to the port unchanged`() {
        val querySlot = slot<RecurrenceStatsQuery>()
        val query = buildQuery(groupBy = RecurrenceGroupBy.SEVERITY, limit = 5)
        every { statsPort.recurrence(capture(querySlot)) } returns emptyList()

        useCase.execute(query)

        assertThat(querySlot.captured.groupBy).isEqualTo(RecurrenceGroupBy.SEVERITY)
        assertThat(querySlot.captured.limit).isEqualTo(5)
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
    fun `should throw when limit exceeds 100`() {
        val query = buildQuery(limit = 101)

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("limit must be")
    }

    @Test
    fun `should throw when limit is zero`() {
        val query = buildQuery(limit = 0)

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should accept limit of exactly 100 as valid upper bound`() {
        val query = buildQuery(limit = 100)
        every { statsPort.recurrence(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should return empty list when port returns empty`() {
        val query = buildQuery()
        every { statsPort.recurrence(query) } returns emptyList()

        val result = useCase.execute(query)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should not call port when validation fails`() {
        val query = buildQuery(from = now, to = now.minusSeconds(3600))

        assertThatThrownBy { useCase.execute(query) }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { statsPort.recurrence(any()) }
    }

    @Test
    fun `should support all groupBy variants`() {
        RecurrenceGroupBy.entries.forEach { groupBy ->
            val query = buildQuery(groupBy = groupBy)
            every { statsPort.recurrence(query) } returns emptyList()

            val result = useCase.execute(query)

            assertThat(result).isNotNull
        }
    }
}

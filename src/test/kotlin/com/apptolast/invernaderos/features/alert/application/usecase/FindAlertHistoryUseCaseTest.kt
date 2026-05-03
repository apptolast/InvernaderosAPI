package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertTransition
import com.apptolast.invernaderos.features.alert.domain.model.TransitionKind
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEventsQuery
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertHistoryQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.PagedResult
import com.apptolast.invernaderos.features.shared.domain.model.SortOrder
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class FindAlertHistoryUseCaseTest {

    private val historyQueryPort = mockk<AlertHistoryQueryPort>()
    private val useCase = FindAlertHistoryUseCaseImpl(historyQueryPort)

    private val tenantId = TenantId(10L)
    private val now = Instant.parse("2026-01-01T12:00:00Z")

    private fun buildTransition(id: Long = 1L, toResolved: Boolean = false) = AlertTransition(
        transitionId = id,
        at = now,
        fromResolved = !toResolved,
        toResolved = toResolved,
        source = AlertSignalSource.MQTT,
        rawValue = "1",
        actor = AlertActor.Device(null),
        alertId = 100L,
        alertCode = "ALT-00001",
        alertMessage = "Test alert",
        alertTypeId = null,
        alertTypeName = null,
        severityId = null,
        severityName = null,
        severityLevel = null,
        severityColor = null,
        sectorId = 20L,
        sectorCode = "SEC-001",
        greenhouseId = 5L,
        greenhouseName = "Greenhouse Norte",
        tenantId = tenantId.value,
        previousTransitionAt = null,
        episodeStartedAt = null,
        episodeDurationSeconds = null,
        occurrenceNumber = 1L,
        totalTransitionsSoFar = 1L,
    )

    @Test
    fun `should return transitions for a valid alertId and tenantId`() {
        val expected = listOf(buildTransition(1L), buildTransition(2L))
        every { historyQueryPort.findTransitionsByAlertId(100L, tenantId, SortOrder.ASC) } returns expected

        val result = useCase.findTransitionsByAlertId(100L, tenantId, SortOrder.ASC)

        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should propagate tenantId to the port unchanged`() {
        val tenantSlot = slot<TenantId>()
        every { historyQueryPort.findTransitionsByAlertId(any(), capture(tenantSlot), any()) } returns emptyList()

        useCase.findTransitionsByAlertId(42L, TenantId(999L), SortOrder.DESC)

        assertThat(tenantSlot.captured.value).isEqualTo(999L)
    }

    @Test
    fun `should propagate ASC order to the port`() {
        val orderSlot = slot<SortOrder>()
        every { historyQueryPort.findTransitionsByAlertId(any(), any(), capture(orderSlot)) } returns emptyList()

        useCase.findTransitionsByAlertId(1L, tenantId, SortOrder.ASC)

        assertThat(orderSlot.captured).isEqualTo(SortOrder.ASC)
    }

    @Test
    fun `should propagate DESC order to the port`() {
        val orderSlot = slot<SortOrder>()
        every { historyQueryPort.findTransitionsByAlertId(any(), any(), capture(orderSlot)) } returns emptyList()

        useCase.findTransitionsByAlertId(1L, tenantId, SortOrder.DESC)

        assertThat(orderSlot.captured).isEqualTo(SortOrder.DESC)
    }

    @Test
    fun `should return empty list when port returns empty for findTransitionsByAlertId`() {
        every { historyQueryPort.findTransitionsByAlertId(any(), any(), any()) } returns emptyList()

        val result = useCase.findTransitionsByAlertId(1L, tenantId, SortOrder.ASC)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should throw when alertId is not positive`() {
        assertThatThrownBy { useCase.findTransitionsByAlertId(0L, tenantId, SortOrder.ASC) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("alertId must be positive")
    }

    @Test
    fun `should throw when alertId is negative`() {
        assertThatThrownBy { useCase.findTransitionsByAlertId(-1L, tenantId, SortOrder.ASC) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should delegate findTransitions to port and return result unchanged`() {
        val query = AlertEventsQuery(
            tenantId = tenantId,
            from = now.minusSeconds(3600),
            to = now,
            sources = emptyList(),
            severityIds = emptyList(),
            alertTypeIds = emptyList(),
            sectorIds = emptyList(),
            greenhouseIds = emptyList(),
            codes = emptyList(),
            actorUserIds = emptyList(),
            transitionKind = TransitionKind.ANY,
            page = 0,
            size = 50,
        )
        val pagedResult = PagedResult(
            items = listOf(buildTransition()),
            page = 0,
            size = 50,
            total = 1L,
            hasMore = false,
        )
        every { historyQueryPort.findTransitions(query) } returns pagedResult

        val result = useCase.findTransitions(query)

        assertThat(result).isEqualTo(pagedResult)
        verify(exactly = 1) { historyQueryPort.findTransitions(query) }
    }

    @Test
    fun `should throw when from is after to in findTransitions`() {
        val query = AlertEventsQuery(
            tenantId = tenantId,
            from = now,
            to = now.minusSeconds(1),
            sources = emptyList(),
            severityIds = emptyList(),
            alertTypeIds = emptyList(),
            sectorIds = emptyList(),
            greenhouseIds = emptyList(),
            codes = emptyList(),
            actorUserIds = emptyList(),
            transitionKind = TransitionKind.ANY,
            page = 0,
            size = 50,
        )

        assertThatThrownBy { useCase.findTransitions(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("from must not be after to")
    }

    @Test
    fun `should throw when page is negative in findTransitions`() {
        val query = AlertEventsQuery(
            tenantId = tenantId,
            from = now.minusSeconds(3600),
            to = now,
            sources = emptyList(),
            severityIds = emptyList(),
            alertTypeIds = emptyList(),
            sectorIds = emptyList(),
            greenhouseIds = emptyList(),
            codes = emptyList(),
            actorUserIds = emptyList(),
            transitionKind = TransitionKind.ANY,
            page = -1,
            size = 50,
        )

        assertThatThrownBy { useCase.findTransitions(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("page must be non-negative")
    }

    @Test
    fun `should throw when size exceeds 200 in findTransitions`() {
        val query = AlertEventsQuery(
            tenantId = tenantId,
            from = now.minusSeconds(3600),
            to = now,
            sources = emptyList(),
            severityIds = emptyList(),
            alertTypeIds = emptyList(),
            sectorIds = emptyList(),
            greenhouseIds = emptyList(),
            codes = emptyList(),
            actorUserIds = emptyList(),
            transitionKind = TransitionKind.ANY,
            page = 0,
            size = 201,
        )

        assertThatThrownBy { useCase.findTransitions(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("size must be")
    }

    @Test
    fun `should throw when size is zero in findTransitions`() {
        val query = AlertEventsQuery(
            tenantId = tenantId,
            from = now.minusSeconds(3600),
            to = now,
            sources = emptyList(),
            severityIds = emptyList(),
            alertTypeIds = emptyList(),
            sectorIds = emptyList(),
            greenhouseIds = emptyList(),
            codes = emptyList(),
            actorUserIds = emptyList(),
            transitionKind = TransitionKind.ANY,
            page = 0,
            size = 0,
        )

        assertThatThrownBy { useCase.findTransitions(query) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should accept size of exactly 200 in findTransitions`() {
        val query = AlertEventsQuery(
            tenantId = tenantId,
            from = now.minusSeconds(3600),
            to = now,
            sources = emptyList(),
            severityIds = emptyList(),
            alertTypeIds = emptyList(),
            sectorIds = emptyList(),
            greenhouseIds = emptyList(),
            codes = emptyList(),
            actorUserIds = emptyList(),
            transitionKind = TransitionKind.ANY,
            page = 0,
            size = 200,
        )
        every { historyQueryPort.findTransitions(query) } returns PagedResult(
            items = emptyList(), page = 0, size = 200, total = 0L, hasMore = false
        )

        val result = useCase.findTransitions(query)

        assertThat(result.size).isEqualTo(200)
    }

    @Test
    fun `should return empty paged result when port returns empty for findTransitions`() {
        val query = AlertEventsQuery(
            tenantId = tenantId,
            from = now.minusSeconds(3600),
            to = now,
            sources = emptyList(),
            severityIds = emptyList(),
            alertTypeIds = emptyList(),
            sectorIds = emptyList(),
            greenhouseIds = emptyList(),
            codes = emptyList(),
            actorUserIds = emptyList(),
            transitionKind = TransitionKind.ANY,
            page = 0,
            size = 50,
        )
        every { historyQueryPort.findTransitions(query) } returns PagedResult(
            items = emptyList(), page = 0, size = 50, total = 0L, hasMore = false
        )

        val result = useCase.findTransitions(query)

        assertThat(result.items).isEmpty()
        assertThat(result.total).isZero()
        assertThat(result.hasMore).isFalse()
    }
}

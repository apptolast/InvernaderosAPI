package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.AlertTransition
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEventsQuery
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertHistoryUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertHistoryQueryPort
import com.apptolast.invernaderos.features.shared.domain.model.PagedResult
import com.apptolast.invernaderos.features.shared.domain.model.SortOrder
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

private const val MAX_PAGE_SIZE = 200
private const val DEFAULT_PAGE_SIZE = 50

/**
 * Plain Kotlin use case for alert history queries. Validates inputs and delegates
 * to [AlertHistoryQueryPort]. No Spring annotations — wiring is in AlertModuleConfig.
 */
class FindAlertHistoryUseCaseImpl(
    private val historyQueryPort: AlertHistoryQueryPort,
) : FindAlertHistoryUseCase {

    override fun findTransitionsByAlertId(
        alertId: Long,
        tenantId: TenantId,
        order: SortOrder,
    ): List<AlertTransition> {
        require(alertId > 0) { "alertId must be positive, got $alertId" }
        return historyQueryPort.findTransitionsByAlertId(alertId, tenantId, order)
    }

    override fun findTransitions(query: AlertEventsQuery): PagedResult<AlertTransition> {
        require(!query.from.isAfter(query.to)) { "from must not be after to" }
        require(query.page >= 0) { "page must be non-negative" }
        require(query.size in 1..MAX_PAGE_SIZE) { "size must be 1...$MAX_PAGE_SIZE" }
        return historyQueryPort.findTransitions(query)
    }
}

package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.model.AlertTransition
import com.apptolast.invernaderos.features.alert.domain.model.query.AlertEventsQuery
import com.apptolast.invernaderos.features.shared.domain.model.PagedResult
import com.apptolast.invernaderos.features.shared.domain.model.SortOrder
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface FindAlertHistoryUseCase {
    fun findTransitionsByAlertId(alertId: Long, tenantId: TenantId, order: SortOrder): List<AlertTransition>
    fun findTransitions(query: AlertEventsQuery): PagedResult<AlertTransition>
}

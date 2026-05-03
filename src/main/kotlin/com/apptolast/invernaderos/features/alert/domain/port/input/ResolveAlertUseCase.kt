package com.apptolast.invernaderos.features.alert.domain.port.input

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface ResolveAlertUseCase {
    fun resolve(id: Long, tenantId: TenantId, resolvedByUserId: Long?): Either<AlertError, Alert>
    fun reopen(id: Long, tenantId: TenantId, actorUserId: Long? = null): Either<AlertError, Alert>
}

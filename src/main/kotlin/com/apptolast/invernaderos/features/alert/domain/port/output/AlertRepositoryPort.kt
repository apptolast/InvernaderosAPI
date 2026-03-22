package com.apptolast.invernaderos.features.alert.domain.port.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface AlertRepositoryPort {
    fun findByIdAndTenantId(id: Long, tenantId: TenantId): Alert?
    fun findAllByTenantId(tenantId: TenantId): List<Alert>
    fun save(alert: Alert): Alert
    fun delete(id: Long, tenantId: TenantId): Boolean
}

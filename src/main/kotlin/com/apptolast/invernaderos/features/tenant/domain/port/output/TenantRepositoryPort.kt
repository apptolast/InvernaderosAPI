package com.apptolast.invernaderos.features.tenant.domain.port.output

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.port.input.TenantFilter

interface TenantRepositoryPort {
    fun findById(id: TenantId): Tenant?
    fun findAll(filter: TenantFilter): List<Tenant>
    fun save(tenant: Tenant): Tenant
    fun delete(id: TenantId): Boolean
    fun existsByName(name: String): Boolean
    fun existsByEmail(email: String): Boolean
}

package com.apptolast.invernaderos.features.user.domain.port.output

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.model.User

interface UserRepositoryPort {
    fun findByIdAndTenantId(id: Long, tenantId: TenantId): User?
    fun findAllByTenantId(tenantId: TenantId): List<User>
    fun save(user: User, passwordHash: String? = null): User
    fun delete(id: Long, tenantId: TenantId): Boolean
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}

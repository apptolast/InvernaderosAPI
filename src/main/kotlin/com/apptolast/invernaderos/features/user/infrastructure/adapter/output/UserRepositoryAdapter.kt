package com.apptolast.invernaderos.features.user.infrastructure.adapter.output

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.UserRepository
import com.apptolast.invernaderos.features.user.domain.model.User
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import com.apptolast.invernaderos.features.user.dto.mapper.toDomain
import com.apptolast.invernaderos.features.user.dto.mapper.toEntity
import org.springframework.stereotype.Component

@Component
class UserRepositoryAdapter(
    private val jpaRepository: UserRepository
) : UserRepositoryPort {

    override fun findByIdAndTenantId(id: Long, tenantId: TenantId): User? {
        return jpaRepository.findByIdAndTenantId(id, tenantId.value)?.toDomain()
    }

    override fun findAllByTenantId(tenantId: TenantId): List<User> {
        return jpaRepository.findByTenantId(tenantId.value).map { it.toDomain() }
    }

    override fun save(user: User, passwordHash: String?): User {
        val resolvedHash = when {
            passwordHash != null -> passwordHash
            user.id != null -> {
                val existing = jpaRepository.findById(user.id)
                if (existing.isPresent) existing.get().passwordHash
                else throw IllegalStateException("Cannot update user ${user.id}: not found in DB and no passwordHash provided")
            }
            else -> throw IllegalStateException("passwordHash is required when creating a new user")
        }
        val entity = user.toEntity(resolvedHash)
        return jpaRepository.save(entity).toDomain()
    }

    override fun delete(id: Long, tenantId: TenantId): Boolean {
        val entity = jpaRepository.findByIdAndTenantId(id, tenantId.value) ?: return false
        jpaRepository.delete(entity)
        return true
    }

    override fun existsByUsername(username: String): Boolean {
        return jpaRepository.existsByUsername(username)
    }

    override fun existsByEmail(email: String): Boolean {
        return jpaRepository.existsByEmail(email)
    }
}

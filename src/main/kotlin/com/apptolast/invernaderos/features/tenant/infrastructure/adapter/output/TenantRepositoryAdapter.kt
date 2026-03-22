package com.apptolast.invernaderos.features.tenant.infrastructure.adapter.output

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.tenant.TenantRepository
import com.apptolast.invernaderos.features.tenant.domain.model.Tenant
import com.apptolast.invernaderos.features.tenant.domain.model.TenantStatus
import com.apptolast.invernaderos.features.tenant.domain.port.input.TenantFilter
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantRepositoryPort
import com.apptolast.invernaderos.features.tenant.dto.mapper.toDomain
import com.apptolast.invernaderos.features.tenant.dto.mapper.toEntity
import com.apptolast.invernaderos.features.tenant.dto.mapper.toIsActive
import org.springframework.stereotype.Component

@Component
class TenantRepositoryAdapter(
    private val jpaRepository: TenantRepository
) : TenantRepositoryPort {

    override fun findById(id: TenantId): Tenant? {
        return jpaRepository.findById(id.value).map { it.toDomain() }.orElse(null)
    }

    override fun findAll(filter: TenantFilter): List<Tenant> {
        val entities = when {
            filter.search != null -> jpaRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                filter.search, filter.search
            )
            filter.province != null -> jpaRepository.findByProvince(filter.province)
            filter.status != null -> {
                val isActive = filter.status.toIsActive()
                if (isActive != null) jpaRepository.findByIsActive(isActive) else jpaRepository.findAll()
            }
            else -> jpaRepository.findAll()
        }
        return entities.map { it.toDomain() }
    }

    override fun save(tenant: Tenant): Tenant {
        val entity = tenant.toEntity()
        return jpaRepository.save(entity).toDomain()
    }

    override fun delete(id: TenantId): Boolean {
        if (jpaRepository.existsById(id.value)) {
            jpaRepository.deleteById(id.value)
            return true
        }
        return false
    }

    override fun existsByName(name: String): Boolean {
        return jpaRepository.findByName(name) != null
    }

    override fun existsByEmail(email: String): Boolean {
        return jpaRepository.findByEmail(email) != null
    }
}

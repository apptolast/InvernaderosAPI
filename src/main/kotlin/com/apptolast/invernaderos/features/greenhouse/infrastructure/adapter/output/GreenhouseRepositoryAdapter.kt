package com.apptolast.invernaderos.features.greenhouse.infrastructure.adapter.output

import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.greenhouse.dto.mapper.toDomain
import com.apptolast.invernaderos.features.greenhouse.dto.mapper.toEntity
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component

@Component
class GreenhouseRepositoryAdapter(
    private val jpaRepository: GreenhouseRepository
) : GreenhouseRepositoryPort {

    override fun findByIdAndTenantId(id: GreenhouseId, tenantId: TenantId): Greenhouse? {
        return jpaRepository.findByIdAndTenantId(id.value, tenantId.value)?.toDomain()
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Greenhouse> {
        return jpaRepository.findByTenantId(tenantId.value).map { it.toDomain() }
    }

    override fun save(greenhouse: Greenhouse): Greenhouse {
        val entity = greenhouse.toEntity()
        return jpaRepository.save(entity).toDomain()
    }

    override fun delete(id: GreenhouseId, tenantId: TenantId): Boolean {
        val entity = jpaRepository.findByIdAndTenantId(id.value, tenantId.value) ?: return false
        jpaRepository.delete(entity)
        return true
    }

    override fun existsByNameAndTenantId(name: String, tenantId: TenantId): Boolean {
        return jpaRepository.findByTenantIdAndName(tenantId.value, name) != null
    }
}

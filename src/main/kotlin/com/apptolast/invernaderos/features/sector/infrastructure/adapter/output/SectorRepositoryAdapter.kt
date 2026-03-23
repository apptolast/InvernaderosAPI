package com.apptolast.invernaderos.features.sector.infrastructure.adapter.output

import com.apptolast.invernaderos.features.sector.SectorRepository
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorRepositoryPort
import com.apptolast.invernaderos.features.sector.dto.mapper.toDomain
import com.apptolast.invernaderos.features.sector.dto.mapper.toEntity
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component

@Component
class SectorRepositoryAdapter(
    private val jpaRepository: SectorRepository
) : SectorRepositoryPort {

    override fun findByIdAndTenantId(id: SectorId, tenantId: TenantId): Sector? {
        val entity = jpaRepository.findById(id.value).orElse(null) ?: return null
        if (entity.tenantId != tenantId.value) return null
        return entity.toDomain()
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Sector> {
        return jpaRepository.findByTenantId(tenantId.value).map { it.toDomain() }
    }

    override fun findAllByGreenhouseId(greenhouseId: GreenhouseId): List<Sector> {
        return jpaRepository.findByGreenhouseId(greenhouseId.value).map { it.toDomain() }
    }

    override fun save(sector: Sector): Sector {
        val entity = sector.toEntity()
        val saved = jpaRepository.save(entity)
        // Reload with EntityGraph to get greenhouse relation
        return jpaRepository.findById(saved.id!!).orElseThrow().toDomain()
    }

    override fun delete(id: SectorId, tenantId: TenantId): Boolean {
        val entity = jpaRepository.findById(id.value).orElse(null) ?: return false
        if (entity.tenantId != tenantId.value) return false
        jpaRepository.delete(entity)
        return true
    }
}

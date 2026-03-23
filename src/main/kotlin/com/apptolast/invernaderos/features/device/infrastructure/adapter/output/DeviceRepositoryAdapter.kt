package com.apptolast.invernaderos.features.device.infrastructure.adapter.output

import com.apptolast.invernaderos.features.device.DeviceRepository
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
import com.apptolast.invernaderos.features.device.dto.mapper.toDomain
import com.apptolast.invernaderos.features.device.dto.mapper.toEntity
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component

@Component
class DeviceRepositoryAdapter(
    private val jpaRepository: DeviceRepository
) : DeviceRepositoryPort {

    override fun findByIdAndTenantId(id: DeviceId, tenantId: TenantId): Device? {
        val entity = jpaRepository.findById(id.value).orElse(null) ?: return null
        if (entity.tenantId != tenantId.value) return null
        return entity.toDomain()
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Device> {
        return jpaRepository.findByTenantId(tenantId.value).map { it.toDomain() }
    }

    override fun findAllBySectorId(sectorId: SectorId): List<Device> {
        return jpaRepository.findBySectorId(sectorId.value).map { it.toDomain() }
    }

    override fun save(device: Device): Device {
        val entity = device.toEntity()
        val saved = jpaRepository.save(entity)
        // Reload with EntityGraph to get catalog relations
        return jpaRepository.findById(saved.id!!).orElseThrow().toDomain()
    }

    override fun delete(id: DeviceId, tenantId: TenantId): Boolean {
        val entity = jpaRepository.findById(id.value).orElse(null) ?: return false
        if (entity.tenantId != tenantId.value) return false
        jpaRepository.delete(entity)
        return true
    }
}

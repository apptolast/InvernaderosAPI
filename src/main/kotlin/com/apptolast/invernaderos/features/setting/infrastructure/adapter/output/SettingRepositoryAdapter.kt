package com.apptolast.invernaderos.features.setting.infrastructure.adapter.output

import com.apptolast.invernaderos.features.setting.SettingRepository
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.setting.dto.mapper.toDomain
import com.apptolast.invernaderos.features.setting.dto.mapper.toEntity
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component

@Component
class SettingRepositoryAdapter(
    private val jpaRepository: SettingRepository
) : SettingRepositoryPort {

    override fun findByIdAndTenantId(id: SettingId, tenantId: TenantId): Setting? {
        val entity = jpaRepository.findById(id.value).orElse(null) ?: return null
        if (entity.tenantId != tenantId.value) return null
        return entity.toDomain()
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Setting> {
        return jpaRepository.findByTenantId(tenantId.value).map { it.toDomain() }
    }

    override fun findAllBySectorId(sectorId: SectorId): List<Setting> {
        return jpaRepository.findBySectorId(sectorId.value).map { it.toDomain() }
    }

    override fun findActiveBySectorId(sectorId: SectorId): List<Setting> {
        return jpaRepository.findBySectorIdAndIsActive(sectorId.value, true).map { it.toDomain() }
    }

    override fun findBySectorIdAndParameterId(sectorId: SectorId, parameterId: Short): List<Setting> {
        return jpaRepository.findBySectorIdAndParameterId(sectorId.value, parameterId).map { it.toDomain() }
    }

    override fun findBySectorIdAndActuatorStateId(sectorId: SectorId, actuatorStateId: Short): List<Setting> {
        return jpaRepository.findBySectorIdAndActuatorStateId(sectorId.value, actuatorStateId).map { it.toDomain() }
    }

    override fun findBySectorParameterAndActuatorState(
        sectorId: SectorId,
        parameterId: Short,
        actuatorStateId: Short
    ): Setting? {
        return jpaRepository.findBySectorIdAndParameterIdAndActuatorStateId(
            sectorId.value, parameterId, actuatorStateId
        )?.toDomain()
    }

    override fun save(setting: Setting): Setting {
        val entity = setting.toEntity()
        val saved = jpaRepository.save(entity)
        // Reload with EntityGraph to get catalog relations
        return jpaRepository.findById(saved.id!!).orElseThrow().toDomain()
    }

    override fun delete(id: SettingId, tenantId: TenantId): Boolean {
        val entity = jpaRepository.findById(id.value).orElse(null) ?: return false
        if (entity.tenantId != tenantId.value) return false
        jpaRepository.delete(entity)
        return true
    }
}

package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.dto.mapper.toDomain
import com.apptolast.invernaderos.features.alert.dto.mapper.toEntity
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component

@Component
class AlertRepositoryAdapter(
    private val jpaRepository: AlertRepository
) : AlertRepositoryPort {

    override fun findByIdAndTenantId(id: Long, tenantId: TenantId): Alert? {
        val entity = jpaRepository.findById(id).orElse(null) ?: return null
        if (entity.tenantId != tenantId.value) return null
        return entity.toDomain()
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Alert> {
        return jpaRepository.findByTenantId(tenantId.value).map { it.toDomain() }
    }

    override fun save(alert: Alert): Alert {
        val entity = alert.toEntity()
        val saved = jpaRepository.save(entity)
        // Reload with EntityGraph to get all relations
        val savedId = saved.id ?: throw IllegalStateException("Alert ID cannot be null after save")
        return jpaRepository.findById(savedId).orElseThrow().toDomain()
    }

    override fun delete(id: Long, tenantId: TenantId): Boolean {
        val entity = jpaRepository.findById(id).orElse(null) ?: return false
        if (entity.tenantId != tenantId.value) return false
        jpaRepository.delete(entity)
        return true
    }
}

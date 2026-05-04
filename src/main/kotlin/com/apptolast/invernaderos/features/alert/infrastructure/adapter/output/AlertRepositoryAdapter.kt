package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.dto.mapper.toDomain
import com.apptolast.invernaderos.features.alert.dto.mapper.toEntity
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component

@Component
class AlertRepositoryAdapter(
    private val jpaRepository: AlertRepository,
    @PersistenceContext(unitName = "metadataPersistenceUnit")
    private val entityManager: EntityManager
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
        val saved = if (alert.id == null) {
            jpaRepository.save(alert.toEntity())
        } else {
            val managed = jpaRepository.findById(alert.id).orElseThrow {
                IllegalStateException("Alert with ID ${alert.id} cannot be found before update")
            }
            managed.applyScalarValuesFrom(alert)
            managed
        }
        // Force a fresh load with @EntityGraph("Alert.context"): without flush+detach,
        // findById returns the cached managed entity whose LAZY relations (severity,
        // sector, alertType) remain uninitialized. Downstream notification dispatch
        // reads severityLevel and would otherwise see null and drop the push.
        val savedId = saved.id ?: throw IllegalStateException("Alert ID cannot be null after save")
        entityManager.flush()
        entityManager.detach(saved)
        return jpaRepository.findById(savedId).orElseThrow().toDomain()
    }

    private fun com.apptolast.invernaderos.features.alert.Alert.applyScalarValuesFrom(alert: Alert) {
        code = alert.code
        tenantId = alert.tenantId.value
        sectorId = alert.sectorId.value
        alertTypeId = alert.alertTypeId
        severityId = alert.severityId
        message = alert.message
        description = alert.description
        clientName = alert.clientName
        isResolved = alert.isResolved
        resolvedAt = alert.resolvedAt
        resolvedByUserId = alert.resolvedByUserId
        updatedAt = alert.updatedAt
    }

    override fun delete(id: Long, tenantId: TenantId): Boolean {
        val entity = jpaRepository.findById(id).orElse(null) ?: return false
        if (entity.tenantId != tenantId.value) return false
        jpaRepository.delete(entity)
        return true
    }

    override fun countUnresolvedBySectorAndTenant(sectorId: Long, tenantId: TenantId): Long {
        return jpaRepository.countByTenantIdAndSectorIdAndIsResolvedFalse(tenantId.value, sectorId)
    }
}

package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertByCodeRepositoryPort
import com.apptolast.invernaderos.features.alert.dto.mapper.toDomain
import com.apptolast.invernaderos.features.alert.dto.mapper.toEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component

@Component
class AlertByCodeRepositoryAdapter(
    private val jpaRepository: AlertRepository,
    @PersistenceContext(unitName = "metadataPersistenceUnit")
    private val entityManager: EntityManager
) : AlertByCodeRepositoryPort {

    override fun findByCode(code: String): Alert? =
        jpaRepository.findByCode(code).orElse(null)?.toDomain()

    override fun save(alert: Alert): Alert {
        val entity = alert.toEntity()
        val saved = jpaRepository.save(entity)
        // Force a fresh load with @EntityGraph("Alert.context"): without flush+detach,
        // findById returns the cached managed entity whose LAZY relations (severity,
        // sector, alertType) remain uninitialized. Downstream notification dispatch
        // reads severityLevel and would otherwise see null and drop the push.
        val savedId = saved.id ?: throw IllegalStateException("Alert ID cannot be null after save")
        entityManager.flush()
        entityManager.detach(saved)
        return jpaRepository.findById(savedId).orElseThrow().toDomain()
    }
}

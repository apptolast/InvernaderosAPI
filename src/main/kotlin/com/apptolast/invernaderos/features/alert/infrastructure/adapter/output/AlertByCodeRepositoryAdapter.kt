package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertByCodeRepositoryPort
import com.apptolast.invernaderos.features.alert.dto.mapper.toDomain
import com.apptolast.invernaderos.features.alert.dto.mapper.toEntity
import org.springframework.stereotype.Component

@Component
class AlertByCodeRepositoryAdapter(
    private val jpaRepository: AlertRepository
) : AlertByCodeRepositoryPort {

    override fun findByCode(code: String): Alert? =
        jpaRepository.findByCode(code).orElse(null)?.toDomain()

    override fun save(alert: Alert): Alert {
        val entity = alert.toEntity()
        val saved = jpaRepository.save(entity)
        // Reload with EntityGraph to get all lazy relations populated
        val savedId = saved.id ?: throw IllegalStateException("Alert ID cannot be null after save")
        return jpaRepository.findById(savedId).orElseThrow().toDomain()
    }
}

package com.apptolast.invernaderos.repositories.metadata

import com.apptolast.invernaderos.entities.metadata.entity.Greenhouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GreenhouseRepository : JpaRepository<Greenhouse, UUID> {
    fun findByTenantId(tenantId: UUID): List<Greenhouse>
    fun findByIsActive(isActive: Boolean): List<Greenhouse>
}
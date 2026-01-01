package com.apptolast.invernaderos.features.sector

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SectorRepository : JpaRepository<Sector, UUID> {
    fun findByGreenhouseId(greenhouseId: UUID): List<Sector>
    fun findByVariety(variety: String): List<Sector>
}

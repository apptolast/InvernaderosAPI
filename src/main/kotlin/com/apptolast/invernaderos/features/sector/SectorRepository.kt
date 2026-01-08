package com.apptolast.invernaderos.features.sector

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SectorRepository : JpaRepository<Sector, Long> {
    fun findByGreenhouseId(greenhouseId: Long): List<Sector>
    fun findByGreenhouseIdIn(greenhouseIds: Collection<Long>): List<Sector>
    fun findByVariety(variety: String): List<Sector>
}

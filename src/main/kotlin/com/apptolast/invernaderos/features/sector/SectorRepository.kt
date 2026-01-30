package com.apptolast.invernaderos.features.sector

import java.util.Optional
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SectorRepository : JpaRepository<Sector, Long> {

    @EntityGraph(value = "Sector.withGreenhouse")
    fun findByTenantId(tenantId: Long): List<Sector>

    @EntityGraph(value = "Sector.withGreenhouse")
    fun findByGreenhouseId(greenhouseId: Long): List<Sector>

    @EntityGraph(value = "Sector.withGreenhouse")
    fun findByGreenhouseIdIn(greenhouseIds: Collection<Long>): List<Sector>

    @EntityGraph(value = "Sector.withGreenhouse")
    fun findByTenantIdAndGreenhouseId(tenantId: Long, greenhouseId: Long): List<Sector>

    @EntityGraph(value = "Sector.withGreenhouse")
    fun findByName(name: String): List<Sector>

    @EntityGraph(value = "Sector.withGreenhouse")
    override fun findById(id: Long): Optional<Sector>
}

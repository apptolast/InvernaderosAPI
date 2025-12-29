package com.apptolast.invernaderos.features.setpoint

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetpointRepository : JpaRepository<Setpoint, UUID> {

    @EntityGraph(value = "Setpoint.context")
    fun findByGreenhouseId(greenhouseId: UUID): List<Setpoint>

    @EntityGraph(value = "Setpoint.context")
    fun findByTenantId(tenantId: UUID): List<Setpoint>

    fun findByGreenhouseIdAndIsActive(greenhouseId: UUID, isActive: Boolean): List<Setpoint>

    fun findByGreenhouseIdAndParameter(greenhouseId: UUID, parameter: SetpointParameter): List<Setpoint>

    fun findByGreenhouseIdAndPeriod(greenhouseId: UUID, period: SetpointPeriod): List<Setpoint>

    fun findByGreenhouseIdAndSector(greenhouseId: UUID, sector: String): List<Setpoint>

    fun findByGreenhouseIdAndSectorIsNull(greenhouseId: UUID): List<Setpoint>

    @Query("SELECT s FROM Setpoint s WHERE s.greenhouseId = :greenhouseId AND s.parameter = :parameter AND s.period = :period AND (s.sector = :sector OR s.sector IS NULL)")
    fun findByGreenhouseParameterPeriodAndSector(
        greenhouseId: UUID,
        parameter: SetpointParameter,
        period: SetpointPeriod,
        sector: String?
    ): List<Setpoint>

    @Query("SELECT s FROM Setpoint s WHERE s.greenhouseId = :greenhouseId AND s.isActive = true")
    fun findActiveByGreenhouseId(greenhouseId: UUID): List<Setpoint>

    @Query("SELECT COUNT(s) FROM Setpoint s WHERE s.greenhouseId = :greenhouseId AND s.isActive = true")
    fun countActiveByGreenhouseId(greenhouseId: UUID): Long
}

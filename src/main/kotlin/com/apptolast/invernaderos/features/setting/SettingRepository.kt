package com.apptolast.invernaderos.features.setting

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SettingRepository : JpaRepository<Setting, UUID> {
    fun findByGreenhouseId(greenhouseId: UUID): List<Setting>
    fun findByTenantId(tenantId: UUID): List<Setting>
    fun findByGreenhouseIdAndIsActive(greenhouseId: UUID, isActive: Boolean): List<Setting>
    fun findByGreenhouseIdAndParameterId(greenhouseId: UUID, parameterId: Short): List<Setting>
    fun findByGreenhouseIdAndPeriodId(greenhouseId: UUID, periodId: Short): List<Setting>
    fun findByGreenhouseIdAndParameterIdAndPeriodId(
        greenhouseId: UUID,
        parameterId: Short,
        periodId: Short
    ): Setting?
}

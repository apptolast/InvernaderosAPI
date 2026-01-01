package com.apptolast.invernaderos.features.device

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceRepository : JpaRepository<Device, UUID> {
    fun findByTenantId(tenantId: UUID): List<Device>
    fun findByGreenhouseId(greenhouseId: UUID): List<Device>
    fun findByTenantIdAndIsActive(tenantId: UUID, isActive: Boolean): List<Device>
    fun findByGreenhouseIdAndIsActive(greenhouseId: UUID, isActive: Boolean): List<Device>
    fun findByCategoryId(categoryId: Short): List<Device>
    fun findByTypeId(typeId: Short): List<Device>
    fun findByGreenhouseIdAndCategoryId(greenhouseId: UUID, categoryId: Short): List<Device>
}

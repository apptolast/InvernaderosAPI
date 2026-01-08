package com.apptolast.invernaderos.features.device

import java.util.Optional
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceRepository : JpaRepository<Device, Long> {

    @EntityGraph(value = "Device.withCatalog")
    fun findByTenantId(tenantId: Long): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByGreenhouseId(greenhouseId: Long): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    override fun findById(id: Long): Optional<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByTenantIdAndIsActive(tenantId: Long, isActive: Boolean): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByGreenhouseIdAndIsActive(greenhouseId: Long, isActive: Boolean): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByCategoryId(categoryId: Short): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByTypeId(typeId: Short): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByGreenhouseIdAndCategoryId(greenhouseId: Long, categoryId: Short): List<Device>
}

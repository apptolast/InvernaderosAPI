package com.apptolast.invernaderos.features.device

import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceRepository : JpaRepository<Device, UUID> {

    @EntityGraph(value = "Device.withCatalog")
    fun findByTenantId(tenantId: UUID): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByGreenhouseId(greenhouseId: UUID): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    override fun findById(id: UUID): Optional<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByTenantIdAndIsActive(tenantId: UUID, isActive: Boolean): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByGreenhouseIdAndIsActive(greenhouseId: UUID, isActive: Boolean): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByCategoryId(categoryId: Short): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByTypeId(typeId: Short): List<Device>

    @EntityGraph(value = "Device.withCatalog")
    fun findByGreenhouseIdAndCategoryId(greenhouseId: UUID, categoryId: Short): List<Device>
}

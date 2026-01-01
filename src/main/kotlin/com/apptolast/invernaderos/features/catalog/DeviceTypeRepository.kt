package com.apptolast.invernaderos.features.catalog

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceTypeRepository : JpaRepository<DeviceType, Short> {
    fun findByName(name: String): DeviceType?
    fun findByCategoryId(categoryId: Short): List<DeviceType>
    fun findByIsActive(isActive: Boolean): List<DeviceType>
    fun findByCategoryIdAndIsActive(categoryId: Short, isActive: Boolean): List<DeviceType>
}

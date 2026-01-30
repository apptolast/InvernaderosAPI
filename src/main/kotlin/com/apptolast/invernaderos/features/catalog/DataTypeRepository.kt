package com.apptolast.invernaderos.features.catalog

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de DataType.
 */
@Repository
interface DataTypeRepository : JpaRepository<DataType, Short> {
    fun findByName(name: String): DataType?
    fun findByIsActive(isActive: Boolean): List<DataType>
    fun findAllByOrderByDisplayOrderAsc(): List<DataType>
    fun existsByName(name: String): Boolean
}

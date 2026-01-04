package com.apptolast.invernaderos.features.catalog

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para DeviceType con EntityGraph para cargar relaciones.
 * Ref: https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html
 */
@Repository
interface DeviceTypeRepository : JpaRepository<DeviceType, Short> {

    @EntityGraph(value = "DeviceType.withRelations")
    override fun findAll(): List<DeviceType>

    @EntityGraph(value = "DeviceType.withRelations")
    fun findByName(name: String): DeviceType?

    @EntityGraph(value = "DeviceType.withRelations")
    fun findByCategoryId(categoryId: Short): List<DeviceType>

    @EntityGraph(value = "DeviceType.withRelations")
    fun findByIsActive(isActive: Boolean): List<DeviceType>

    @EntityGraph(value = "DeviceType.withRelations")
    fun findByCategoryIdAndIsActive(categoryId: Short, isActive: Boolean): List<DeviceType>
}

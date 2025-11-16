package com.apptolast.invernaderos.repositories.metadata

import com.apptolast.invernaderos.entities.metadata.catalog.AlertType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AlertTypeRepository : JpaRepository<AlertType, Short> {

    /**
     * Busca un tipo de alerta por nombre
     * Ej: findByName("SENSOR_OFFLINE") → AlertType(id=1, name="SENSOR_OFFLINE")
     */
    fun findByName(name: String): AlertType?

    /**
     * Obtiene todos los tipos de alertas activas
     */
    fun findByIsActiveTrue(): List<AlertType>

    /**
     * Busca tipos de alertas por categoría
     * Ej: findByCategory("SENSOR") → List<AlertType>
     */
    fun findByCategory(category: String): List<AlertType>

    /**
     * Busca tipo de alerta por nombre (case-insensitive)
     */
    @Query("SELECT at FROM AlertType at WHERE LOWER(at.name) = LOWER(:name)")
    fun findByNameIgnoreCase(name: String): AlertType?

    /**
     * Obtiene tipos de alerta filtrados por categoría y activos
     */
    fun findByCategoryAndIsActiveTrue(category: String): List<AlertType>
}

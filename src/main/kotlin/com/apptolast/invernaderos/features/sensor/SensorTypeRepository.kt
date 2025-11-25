package com.apptolast.invernaderos.features.sensor

import com.apptolast.invernaderos.features.catalog.catalog.SensorType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SensorTypeRepository : JpaRepository<SensorType, Short> {

    /**
     * Busca un tipo de sensor por nombre
     * Ej: findByName("TEMPERATURE") → SensorType(id=1, name="TEMPERATURE")
     */
    fun findByName(name: String): SensorType?

    /**
     * Obtiene todos los tipos de sensores activos
     */
    fun findByIsActiveTrue(): List<SensorType>

    /**
     * Busca tipos de sensores por tipo de dato
     * Ej: findByDataType("DECIMAL") → List<SensorType>
     */
    fun findByDataType(dataType: String): List<SensorType>

    /**
     * Busca tipo de sensor por nombre (case-insensitive)
     */
    @Query("SELECT st FROM SensorType st WHERE LOWER(st.name) = LOWER(:name)")
    fun findByNameIgnoreCase(name: String): SensorType?
}

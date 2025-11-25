package com.apptolast.invernaderos.features.actuator

import com.apptolast.invernaderos.features.catalog.catalog.ActuatorType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ActuatorTypeRepository : JpaRepository<ActuatorType, Short> {

    /**
     * Busca un tipo de actuador por nombre
     * Ej: findByName("VENTILATOR") → ActuatorType(id=1, name="VENTILATOR")
     */
    fun findByName(name: String): ActuatorType?

    /**
     * Obtiene todos los tipos de actuadores activos
     */
    fun findByIsActiveTrue(): List<ActuatorType>

    /**
     * Busca actuadores por tipo de control
     * Ej: findByControlType("BINARY") → List<ActuatorType>
     */
    fun findByControlType(controlType: String): List<ActuatorType>

    /**
     * Busca tipo de actuador por nombre (case-insensitive)
     */
    @Query("SELECT at FROM ActuatorType at WHERE LOWER(at.name) = LOWER(:name)")
    fun findByNameIgnoreCase(name: String): ActuatorType?
}

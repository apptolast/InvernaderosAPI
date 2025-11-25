package com.apptolast.invernaderos.features.actuator

import com.apptolast.invernaderos.features.catalog.catalog.ActuatorState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ActuatorStateRepository : JpaRepository<ActuatorState, Short> {

    /**
     * Busca un estado de actuador por nombre
     * Ej: findByName("ON") → ActuatorState(id=2, name="ON")
     */
    fun findByName(name: String): ActuatorState?

    /**
     * Obtiene todos los estados operacionales (ON, AUTO, MANUAL)
     */
    fun findByIsOperationalTrue(): List<ActuatorState>

    /**
     * Obtiene todos los estados ordenados por display_order
     */
    @Query("SELECT as FROM ActuatorState as ORDER BY as.displayOrder ASC")
    fun findAllOrderedByDisplay(): List<ActuatorState>

    /**
     * Busca estado por nombre (case-insensitive)
     */
    @Query("SELECT as FROM ActuatorState as WHERE LOWER(as.name) = LOWER(:name)")
    fun findByNameIgnoreCase(name: String): ActuatorState?
}

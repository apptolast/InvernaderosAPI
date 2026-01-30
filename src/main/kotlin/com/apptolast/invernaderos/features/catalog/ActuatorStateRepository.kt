package com.apptolast.invernaderos.features.catalog

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActuatorStateRepository : JpaRepository<ActuatorState, Short> {
    fun findByName(name: String): ActuatorState?
    fun findByIsOperational(isOperational: Boolean): List<ActuatorState>
    fun findAllByOrderByDisplayOrderAsc(): List<ActuatorState>
}

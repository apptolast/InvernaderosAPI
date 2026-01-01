package com.apptolast.invernaderos.features.catalog

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AlertSeverityRepository : JpaRepository<AlertSeverity, Short> {
    fun findByName(name: String): AlertSeverity?
    fun findByRequiresAction(requiresAction: Boolean): List<AlertSeverity>
    fun findAllByOrderByLevelAsc(): List<AlertSeverity>
}

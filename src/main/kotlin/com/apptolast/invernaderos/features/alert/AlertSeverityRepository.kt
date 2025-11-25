package com.apptolast.invernaderos.features.alert

import com.apptolast.invernaderos.features.catalog.catalog.AlertSeverity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AlertSeverityRepository : JpaRepository<AlertSeverity, Short> {

    /**
     * Busca una severidad por nombre
     * Ej: findByName("CRITICAL") → AlertSeverity(id=4, name="CRITICAL", level=4)
     */
    fun findByName(name: String): AlertSeverity?

    /**
     * Busca severidades por nivel
     * Ej: findByLevel(4) → AlertSeverity(name="CRITICAL")
     */
    fun findByLevel(level: Short): AlertSeverity?

    /**
     * Obtiene severidades que requieren acción inmediata
     */
    fun findByRequiresActionTrue(): List<AlertSeverity>

    /**
     * Obtiene todas las severidades ordenadas por nivel (ascendente)
     */
    @Query("SELECT asev FROM AlertSeverity asev ORDER BY asev.level ASC")
    fun findAllOrderedByLevel(): List<AlertSeverity>

    /**
     * Busca severidad por nombre (case-insensitive)
     */
    @Query("SELECT asev FROM AlertSeverity asev WHERE LOWER(asev.name) = LOWER(:name)")
    fun findByNameIgnoreCase(name: String): AlertSeverity?
}

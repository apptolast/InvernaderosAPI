package com.apptolast.invernaderos.features.tenant

import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de Tenants. Proporciona queries personalizados para búsquedas
 * comunes en sistema multi-tenant.
 */
@Repository
interface TenantRepository : JpaRepository<Tenant, UUID> {

    /**
     * Buscar tenant por MQTT topic prefix. CRÍTICO para validación multi-tenant en MQTT processing.
     *
     * Ejemplo: mqttTopicPrefix = "SARA" → Tenant con MQTT topics "GREENHOUSE/SARA"
     */
    @EntityGraph(value = "Tenant.detail")
    fun findByMqttTopicPrefix(mqttTopicPrefix: String): Tenant?

    /** Buscar tenant por email único. */
    @EntityGraph(value = "Tenant.detail") fun findByEmail(email: String): Tenant?

    /** Buscar tenant por CIF/NIF/Tax ID. */
    @EntityGraph(value = "Tenant.detail") fun findByTaxId(taxId: String): Tenant?

    /** Buscar tenant por nombre de empresa. */
    @EntityGraph(value = "Tenant.detail") fun findByCompanyName(companyName: String): Tenant?

    /** Buscar tenants activos. */
    @EntityGraph(value = "Tenant.detail") fun findByIsActive(isActive: Boolean): List<Tenant>

    /** Buscar tenants por empresa (case-insensitive). */
    @EntityGraph(value = "Tenant.detail")
    @Query(
            "SELECT t FROM Tenant t WHERE LOWER(t.companyName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))"
    )
    fun searchByCompanyName(@Param("searchTerm") searchTerm: String): List<Tenant>

    /** Contar tenants activos. */
    fun countByIsActive(isActive: Boolean): Long
}

package com.apptolast.invernaderos.repositories.metadata

import com.apptolast.invernaderos.entities.metadata.entity.Tenant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository para operaciones CRUD de Tenants.
 * Proporciona queries personalizados para búsquedas comunes en sistema multi-tenant.
 */
@Repository
interface TenantRepository : JpaRepository<Tenant, UUID> {

    /**
     * Buscar tenant por MQTT topic prefix.
     * CRÍTICO para validación multi-tenant en MQTT processing.
     *
     * Ejemplo: mqttTopicPrefix = "SARA" → Tenant con MQTT topics "GREENHOUSE/SARA"
     */
    fun findByMqttTopicPrefix(mqttTopicPrefix: String): Tenant?

    /**
     * Buscar tenant por email único.
     */
    fun findByEmail(email: String): Tenant?

    /**
     * Buscar tenant por CIF/NIF/Tax ID.
     */
    fun findByTaxId(taxId: String): Tenant?

    /**
     * Buscar tenant por nombre de empresa.
     */
    fun findByCompanyName(companyName: String): Tenant?

    /**
     * Buscar tenants activos.
     */
    fun findByIsActive(isActive: Boolean): List<Tenant>

    /**
     * Buscar tenants por empresa (case-insensitive).
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.companyName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    fun searchByCompanyName(@Param("searchTerm") searchTerm: String): List<Tenant>

    /**
     * Contar tenants activos.
     */
    fun countByIsActive(isActive: Boolean): Long
}

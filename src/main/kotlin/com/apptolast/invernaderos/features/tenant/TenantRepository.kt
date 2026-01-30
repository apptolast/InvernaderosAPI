package com.apptolast.invernaderos.features.tenant

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de Tenants.
 * Proporciona queries personalizados para busquedas comunes en sistema multi-tenant.
 */
@Repository
interface TenantRepository : JpaRepository<Tenant, Long> {

    /**
     * Buscar tenant por nombre unico.
     * CRITICO para identificacion MQTT: topic GREENHOUSE/SARA busca tenant con name='SARA'
     */
    fun findByName(name: String): Tenant?

    /**
     * Buscar tenant por email unico.
     */
    fun findByEmail(email: String): Tenant?

    /**
     * Buscar tenants activos.
     */
    fun findByIsActive(isActive: Boolean): List<Tenant>

    /**
     * Buscar por nombre o email (para el buscador de la UI).
     */
    fun findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(name: String, email: String): List<Tenant>

    /**
     * Buscar por provincia.
     */
    fun findByProvince(province: String): List<Tenant>

    /**
     * Contar tenants activos.
     */
    fun countByIsActive(isActive: Boolean): Long
}

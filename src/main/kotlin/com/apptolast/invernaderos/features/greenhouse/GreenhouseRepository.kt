package com.apptolast.invernaderos.features.greenhouse

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de Greenhouses.
 * Proporciona queries personalizados para busquedas comunes en sistema multi-tenant.
 */
@Repository
interface GreenhouseRepository : JpaRepository<Greenhouse, Long> {

    /**
     * Buscar invernaderos por tenant ID.
     */
    fun findByTenantId(tenantId: Long): List<Greenhouse>

    /**
     * Buscar invernaderos activos/inactivos.
     */
    fun findByIsActive(isActive: Boolean): List<Greenhouse>

    /**
     * Buscar invernaderos activos de un tenant especifico.
     * CRITICO para validacion multi-tenant en MQTT processing.
     */
    fun findByTenantIdAndIsActive(tenantId: Long, isActive: Boolean): List<Greenhouse>

    /**
     * Buscar invernadero por nombre dentro de un tenant.
     */
    fun findByTenantIdAndName(tenantId: Long, name: String): Greenhouse?

    /**
     * Buscar un invernadero por su ID y el ID del tenant.
     */
    fun findByIdAndTenantId(id: Long, tenantId: Long): Greenhouse?
}

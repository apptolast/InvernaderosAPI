package com.apptolast.invernaderos.features.greenhouse

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de Greenhouses.
 * Proporciona queries personalizados para busquedas comunes en sistema multi-tenant.
 */
@Repository
interface GreenhouseRepository : JpaRepository<Greenhouse, UUID> {

    /**
     * Buscar invernaderos por tenant ID.
     */
    fun findByTenantId(tenantId: UUID): List<Greenhouse>

    /**
     * Buscar invernaderos activos/inactivos.
     */
    fun findByIsActive(isActive: Boolean): List<Greenhouse>

    /**
     * Buscar invernaderos activos de un tenant especifico.
     * CRITICO para validacion multi-tenant en MQTT processing.
     */
    fun findByTenantIdAndIsActive(tenantId: UUID, isActive: Boolean): List<Greenhouse>

    /**
     * Buscar invernadero por nombre dentro de un tenant.
     */
    fun findByTenantIdAndName(tenantId: UUID, name: String): Greenhouse?
}

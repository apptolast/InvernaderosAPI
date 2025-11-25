package com.apptolast.invernaderos.features.greenhouse

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de Greenhouses. Proporciona queries personalizados para
 * búsquedas comunes en sistema multi-tenant.
 */
@Repository
interface GreenhouseRepository : JpaRepository<Greenhouse, UUID> {

    /** Buscar invernaderos por tenant ID. */
    fun findByTenantId(tenantId: UUID): List<Greenhouse>

    /** Buscar invernaderos activos/inactivos. */
    fun findByIsActive(isActive: Boolean): List<Greenhouse>

    /**
     * Buscar invernaderos activos de un tenant específico. CRÍTICO para validación multi-tenant en
     * MQTT processing.
     *
     * NOTA: No usar @EntityGraph con múltiples colecciones simultáneamente para evitar
     * MultipleBagFetchException. Las colecciones se cargan con lazy loading.
     */
    fun findByTenantIdAndIsActive(tenantId: UUID, isActive: Boolean): List<Greenhouse>

    /** Buscar invernadero por MQTT topic único. */
    fun findByMqttTopic(mqttTopic: String): Greenhouse?

    /** Buscar invernadero por código dentro de un tenant. */
    fun findByTenantIdAndGreenhouseCode(tenantId: UUID, greenhouseCode: String): Greenhouse?
}

package com.apptolast.invernaderos.features.actuator

import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de Actuators. Proporciona queries personalizados para búsquedas
 * comunes en sistema multi-tenant.
 */
@Repository
interface ActuatorRepository : JpaRepository<Actuator, UUID> {

    /** Buscar actuadores por greenhouse ID. */
    @EntityGraph(value = "Actuator.context")
    fun findByGreenhouseId(greenhouseId: UUID): List<Actuator>

    /** Buscar actuadores activos de un greenhouse. */
    @EntityGraph(value = "Actuator.context")
    fun findByGreenhouseIdAndIsActive(greenhouseId: UUID, isActive: Boolean): List<Actuator>

    /** Buscar actuadores por tenant ID. */
    fun findByTenantId(tenantId: UUID): List<Actuator>

    /** Buscar actuadores activos de un tenant. */
    fun findByTenantIdAndIsActive(tenantId: UUID, isActive: Boolean): List<Actuator>

    /** Buscar actuador por device ID único. */
    @EntityGraph(value = "Actuator.context") fun findByDeviceId(deviceId: String): Actuator?

    /** Buscar actuador por código único dentro de un greenhouse. */
    fun findByGreenhouseIdAndActuatorCode(greenhouseId: UUID, actuatorCode: String): Actuator?

    /** Buscar todos los actuadores activos. */
    fun findByIsActive(isActive: Boolean): List<Actuator>

    /** Buscar actuadores por tipo. */
    fun findByActuatorType(actuatorType: String): List<Actuator>

    /** Buscar actuadores por tipo y estado activo. */
    fun findByActuatorTypeAndIsActive(actuatorType: String, isActive: Boolean): List<Actuator>

    /** Buscar actuadores por estado actual. */
    fun findByCurrentState(currentState: String): List<Actuator>

    /** Buscar actuadores de un greenhouse por tipo. */
    fun findByGreenhouseIdAndActuatorType(greenhouseId: UUID, actuatorType: String): List<Actuator>

    /**
     * Query personalizado: Actuadores con última actualización antigua (desconectados). Útil para
     * detectar actuadores que no han reportado estado recientemente.
     */
    @Query(
            """
        SELECT a FROM Actuator a
        WHERE a.isActive = true
        AND (a.lastStatusUpdate IS NULL OR a.lastStatusUpdate < :threshold)
        ORDER BY a.lastStatusUpdate ASC NULLS FIRST
        """
    )
    fun findStaleActuators(@Param("threshold") threshold: java.time.Instant): List<Actuator>

    /** Query personalizado: Actuadores en estado ERROR. */
    @Query("SELECT a FROM Actuator a WHERE a.currentState = 'ERROR' AND a.isActive = true")
    fun findActuatorsInError(): List<Actuator>

    /** Contar actuadores activos de un tenant. */
    fun countByTenantIdAndIsActive(tenantId: UUID, isActive: Boolean): Long

    /** Contar actuadores activos de un greenhouse. */
    fun countByGreenhouseIdAndIsActive(greenhouseId: UUID, isActive: Boolean): Long
}

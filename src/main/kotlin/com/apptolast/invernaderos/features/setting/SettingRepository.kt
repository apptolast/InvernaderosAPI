package com.apptolast.invernaderos.features.setting

import java.util.Optional
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de Settings.
 *
 * Los metodos usan @EntityGraph para cargar las relaciones LAZY (parameter, actuatorState)
 * en una sola query, evitando LazyInitializationException.
 *
 * @see <a href="https://docs.spring.io/spring-data/data-jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html">Spring Data JPA EntityGraph</a>
 */
@Repository
interface SettingRepository : JpaRepository<Setting, Long> {

    @EntityGraph(value = "Setting.withCatalog")
    override fun findById(id: Long): Optional<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findBySectorId(sectorId: Long): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findByTenantId(tenantId: Long): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findBySectorIdAndIsActive(sectorId: Long, isActive: Boolean): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findBySectorIdAndParameterId(sectorId: Long, parameterId: Short): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findBySectorIdAndActuatorStateId(sectorId: Long, actuatorStateId: Short): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findBySectorIdAndParameterIdAndActuatorStateId(
        sectorId: Long,
        parameterId: Short,
        actuatorStateId: Short
    ): Setting?
}

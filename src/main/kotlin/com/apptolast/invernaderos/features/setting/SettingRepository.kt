package com.apptolast.invernaderos.features.setting

import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para operaciones CRUD de Settings.
 *
 * Los métodos usan @EntityGraph para cargar las relaciones LAZY (parameter, period)
 * en una sola query, evitando LazyInitializationException.
 *
 * @see <a href="https://docs.spring.io/spring-data/data-jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html">Spring Data JPA EntityGraph</a>
 */
@Repository
interface SettingRepository : JpaRepository<Setting, UUID> {

    @EntityGraph(value = "Setting.withCatalog")
    override fun findById(id: UUID): Optional<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findByGreenhouseId(greenhouseId: UUID): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findByTenantId(tenantId: UUID): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findByGreenhouseIdAndIsActive(greenhouseId: UUID, isActive: Boolean): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findByGreenhouseIdAndParameterId(greenhouseId: UUID, parameterId: Short): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findByGreenhouseIdAndPeriodId(greenhouseId: UUID, periodId: Short): List<Setting>

    @EntityGraph(value = "Setting.withCatalog")
    fun findByGreenhouseIdAndParameterIdAndPeriodId(
        greenhouseId: UUID,
        parameterId: Short,
        periodId: Short
    ): Setting?
}

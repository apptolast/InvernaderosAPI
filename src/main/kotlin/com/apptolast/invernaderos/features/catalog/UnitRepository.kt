package com.apptolast.invernaderos.features.catalog

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UnitRepository : JpaRepository<Unit, Short> {
    fun findBySymbol(symbol: String): Unit?
    fun findByIsActive(isActive: Boolean): List<Unit>
}
